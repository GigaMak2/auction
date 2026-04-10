package com.example.auction.domain.ai.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.ai.enums.SseEventType;
import com.example.auction.domain.ai.exception.AiErrorEnum;
import java.time.Duration;
import com.example.auction.domain.chat.entity.ChatMessage;
import com.example.auction.domain.chat.entity.ChatRoom;
import com.example.auction.domain.chat.entity.MessageRole;
import com.example.auction.domain.chat.exception.ChatErrorEnum;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import com.example.auction.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient chatClient;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * Streams an AI-generated response to a user's message as Server-Sent Events (SSE).
     *
     * <p>This method validates the chat room and ownership, persists the user's message, then
     * emits a sequence of SSEs representing AI response tokens, optionally a TOPIC event when
     * the room has no title, and a final DONE event. The accumulated assistant response is
     * saved when the stream completes (normal, error, or cancellation).</p>
     *
     * @param roomId  the identifier of the chat room
     * @param userId  the identifier of the requesting user
     * @param content the user's message content to send to the AI
     * @return a Flux of ServerSentEvent<String> that emits TOKEN events for response tokens,
     *         optionally a TOPIC event with the generated title (first message only), and a DONE
     *         event; on AI failure the stream emits an ERROR event with a human-facing message
     *         followed by DONE
     * @throws ServiceErrorException if the chat room is not found or the user is not the room owner
     */
    public Flux<ServerSentEvent<String>> streamMessage(Long roomId, Long userId, String content) {
        // 1. 채팅방 존재 확인 + 소유자 검증
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ServiceErrorException(ChatErrorEnum.CHAT_ROOM_NOT_FOUND));

        if (!chatRoom.getUserId().equals(userId)) {
            throw new ServiceErrorException(ChatErrorEnum.CHAT_ROOM_FORBIDDEN);
        }

        // 2. 유저 메시지 저장
        chatMessageRepository.save(ChatMessage.of(roomId, content, MessageRole.USER));

        boolean isFirstMessage = chatRoom.getTitle() == null;
        StringBuilder fullResponse = new StringBuilder();

        // 3. TOKEN 스트리밍 — Chapter 2의 stream().content() 패턴
        Flux<ServerSentEvent<String>> tokenStream = chatClient.prompt()
                .system("""
                        당신은 중고물품 역경매 플랫폼의 AI 상담사입니다.
                        시세 조회, 판매자 분석, 경쟁 입찰 분석을 도와드립니다.
                        한국어로 답변하세요.
                        """)
                .user(content)
                .stream()
                .content()
                .timeout(Duration.ofSeconds(40)) // 40초 내 응답 없으면 Fallback으로 처리
                .doOnNext(fullResponse::append)
                .map(token -> ServerSentEvent.<String>builder()
                        .event(SseEventType.TOKEN.name())
                        .data(token)
                        .build())
                .doFinally(signalType -> {
                    // 정상 완료 / 에러 / 취소 모든 경우에 누적된 응답 저장
                    if (!fullResponse.isEmpty()) {
                        chatMessageRepository.save(
                                ChatMessage.of(roomId, fullResponse.toString(), MessageRole.ASSISTANT));
                    }
                });

        // 4. TOPIC 이벤트 — 첫 메시지일 때만 채팅방 title 생성
        // Flux.defer: tokenStream 완료 후 구독 시점에 실행 (즉시 실행 방지)
        Flux<ServerSentEvent<String>> topicStream = isFirstMessage
                ? Flux.defer(() -> generateTitle(chatRoom, content))
                : Flux.empty();

        // 5. DONE 이벤트
        Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event(SseEventType.DONE.name())
                        .data("")
                        .build()
        );

        // 6. Fallback — AI 장애 시 ERROR 이벤트로 오류 안내 후 DONE으로 스트림 종료
        return tokenStream
                .concatWith(topicStream)
                .concatWith(doneEvent)
                .onErrorResume(e -> {
                    log.error("[AiService] 스트리밍 오류: {}", e.getMessage());
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event(SseEventType.ERROR.name())
                                    .data(AiErrorEnum.AI_SERVICE_UNAVAILABLE.getMessage())
                                    .build(),
                            ServerSentEvent.<String>builder()
                                    .event(SseEventType.DONE.name())
                                    .data("")
                                    .build()
                    );
                });
    }

    // Chapter 2의 call().content() 패턴 — 채팅방 title 동기 생성
    private Flux<ServerSentEvent<String>> generateTitle(ChatRoom chatRoom, String content) {
        String rawTitle = chatClient.prompt()
                .system("""
                        사용자의 첫 메시지를 보고 채팅방 제목을 10자 이내로 생성하세요.
                        제목만 반환하세요.
                        """)
                .user(content)
                .call()
                .content();

        // AI 응답 정제 — 프롬프트만으로는 길이/null 보장 불가
        String trimmed = (rawTitle != null) ? rawTitle.trim() : "";
        String safeTitle = !trimmed.isBlank()
                ? trimmed.substring(0, Math.min(trimmed.length(), 10))
                : "새 채팅";

        chatRoom.updateTitle(safeTitle);
        chatRoomRepository.save(chatRoom);

        return Flux.just(
                ServerSentEvent.<String>builder()
                        .event(SseEventType.TOPIC.name())
                        .data(safeTitle)
                        .build()
        );
    }
}