package com.example.auction.domain.ai.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.chat.service.ChatContextCacheService;
import com.example.auction.domain.ai.enums.SseEventType;
import com.example.auction.domain.ai.exception.AiErrorEnum;
import com.example.auction.domain.ai.tool.AuctionTools;
import java.time.Duration;
import java.util.List;
import com.example.auction.domain.chat.entity.ChatMessage;
import com.example.auction.domain.chat.entity.ChatRoom;
import com.example.auction.domain.chat.entity.MessageRole;
import com.example.auction.domain.chat.exception.ChatErrorEnum;
import com.example.auction.domain.chat.repository.ChatMessageRepository;
import com.example.auction.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ChatClient chatClient;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final AuctionTools auctionTools; // LLM이 호출할 Tool 묶음
    private final ChatContextCacheService chatContextCacheService; // 대화 컨텍스트 Redis 캐싱

    public Flux<ServerSentEvent<String>> streamMessage(Long roomId, Long userId, String content) {
        // Flux.defer: 모든 로직을 구독 시점에 실행 — 동기 예외가 Flux 에러로 처리되어
        // onErrorResume이 SSE ERROR 이벤트로 반환 (HttpMediaTypeNotAcceptableException 방지)
        return Flux.defer(() -> {
            // 1. content 수동 검증 — @Valid 대신 Flux.defer() 안에서 처리 (SSE MediaType 충돌 방지)
            if (content == null || content.isBlank()) {
                throw new ServiceErrorException(AiErrorEnum.INVALID_MESSAGE_CONTENT);
            }
            if (content.length() > 500) {
                throw new ServiceErrorException(AiErrorEnum.MESSAGE_TOO_LONG);
            }

            // 2. 채팅방 존재 확인 + 소유자 검증
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new ServiceErrorException(ChatErrorEnum.CHAT_ROOM_NOT_FOUND));

            if (!chatRoom.getUserId().equals(userId)) {
                throw new ServiceErrorException(ChatErrorEnum.CHAT_ROOM_FORBIDDEN);
            }

            // 3. Redis에서 이전 대화 컨텍스트 로드 — 유저 메시지 저장 전에 조회해야 현재 메시지 중복 방지
            List<Message> historyMessages = chatContextCacheService.getContext(roomId)
                    .stream()
                    .map(m -> "USER".equals(m.role())
                            ? (Message) new UserMessage(m.content())
                            : new AssistantMessage(m.content()))
                    .toList();

            // 4. 유저 메시지 저장
            chatMessageRepository.save(ChatMessage.of(roomId, content, MessageRole.USER));

            boolean isFirstMessage = chatRoom.getTitle() == null;
            StringBuilder fullResponse = new StringBuilder();

            // 5. TOKEN 스트리밍 — 이전 대화 히스토리 + 현재 메시지, Tool Calling 포함
            // Chapter 2의 stream().content() 패턴, messages()로 컨텍스트 전달
            // 시스템 프롬프트: Chapter 3 역할 부여 + 퓨-샷 기법 적용
            Flux<ServerSentEvent<String>> tokenStream = chatClient.prompt()
                    .system("""
                            ## 역할
                            당신은 중고물품 역경매 플랫폼 전문 AI 상담사입니다.
                            이 플랫폼은 구매자가 경매를 등록하면 판매자들이 입찰하고, 마감 시 최저가 입찰자가 자동 낙찰되는 역경매 구조입니다.
                            시세 조회, 판매자 신뢰도 분석, 경쟁 입찰 분석을 전문으로 합니다.
                            한국어로 답변하세요.

                            ## 금지 사항
                            - max_price(구매자의 예산 상한)는 절대 언급하거나 추론해서는 안 됩니다.
                            - 실제 데이터 없이 시세, 입찰 현황, 판매자 정보를 추측하거나 일반적인 답변을 해서는 안 됩니다.

                            ## 응답 형식
                            - 수치 데이터(입찰가, 낙찰가, 평점 등)는 표로 정리하세요.
                            - 분석 및 조언은 bullet point로 간결하게 작성하세요.

                            ## 응답 예시

                            Q: "노트북 시세 알려줘"
                            A:
                            | 상품명 | 낙찰가 | 낙찰일 |
                            |--------|--------|--------|
                            | 노트북 A | 850,000원 | 2026-04-10 |
                            | 노트북 B | 790,000원 | 2026-04-08 |
                            - 최근 평균 낙찰가는 약 82만원입니다.
                            - 상태가 좋을수록 85만원 이상 입찰을 고려해보세요.

                            Q: "판매자 42번 믿을 수 있어?"
                            A:
                            | 항목 | 내용 |
                            |------|------|
                            | 총 낙찰 횟수 | 15건 |
                            | 평균 평점 | 4.3 / 5.0 |
                            - 거래 이력이 충분하고 평점이 높아 신뢰할 수 있는 판매자입니다.
                            - 후기에서 "빠른 배송"과 "상품 상태 양호"가 자주 언급됩니다.
                            """)
                    .messages(historyMessages) // 이전 대화 히스토리 (Redis 캐시)
                    .user(content)
                    .tools(auctionTools)  // LLM이 필요 시 경매 데이터 조회 Tool 호출
                    .stream()
                    .content()
                    .timeout(Duration.ofSeconds(40)) // 40초 내 응답 없으면 Fallback으로 처리
                    .doOnNext(fullResponse::append)
                    .map(token -> ServerSentEvent.<String>builder()
                            .event(SseEventType.TOKEN.name())
                            .data(token)
                            .build())
                    .doFinally(signalType -> {
                        // 정상 완료(ON_COMPLETE)일 때만 저장 — 에러/취소 시 부분 응답이 다음 턴 컨텍스트 오염 방지
                        if (signalType == reactor.core.publisher.SignalType.ON_COMPLETE && !fullResponse.isEmpty()) {
                            chatMessageRepository.save(
                                    ChatMessage.of(roomId, fullResponse.toString(), MessageRole.ASSISTANT));
                            // 유저 메시지 + AI 응답을 캐시에 추가 (다음 턴 컨텍스트에 활용)
                            chatContextCacheService.appendMessages(roomId, content, fullResponse.toString());
                        } else if (signalType != reactor.core.publisher.SignalType.ON_COMPLETE) {
                            // 실패 시 캐시 evict — 유저 메시지는 DB에 저장됐으나 캐시엔 없으므로
                            // 다음 턴 getContext()가 DB 폴백으로 정확한 이력을 가져오도록 함
                            chatContextCacheService.evict(roomId);
                        }
                    });

            // 6. TOPIC 이벤트 — 첫 메시지일 때만 채팅방 title 생성
            // Flux.defer: tokenStream 완료 후 구독 시점에 실행 (즉시 실행 방지)
            Flux<ServerSentEvent<String>> topicStream = isFirstMessage
                    ? Flux.defer(() -> generateTitle(chatRoom, content))
                    : Flux.empty();

            // 7. DONE 이벤트
            Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                    ServerSentEvent.<String>builder()
                            .event(SseEventType.DONE.name())
                            .data("")
                            .build()
            );

            return tokenStream.concatWith(topicStream).concatWith(doneEvent);
        })
        // 8. Fallback — 검증 실패·AI 장애 시 ERROR 이벤트로 오류 안내 후 DONE으로 스트림 종료
        .onErrorResume(e -> {
            log.error("[AiService] 스트리밍 오류: {}", e.getMessage());
            String errorMessage = (e instanceof ServiceErrorException)
                    ? e.getMessage()
                    : AiErrorEnum.AI_SERVICE_UNAVAILABLE.getMessage();
            return Flux.just(
                    ServerSentEvent.<String>builder()
                            .event(SseEventType.ERROR.name())
                            .data(errorMessage)
                            .build(),
                    ServerSentEvent.<String>builder()
                            .event(SseEventType.DONE.name())
                            .data("")
                            .build()
            );
        });
    }

    // 채팅방 title 생성 — boundedElastic에서 실행해 이벤트 루프 블로킹 방지
    private Flux<ServerSentEvent<String>> generateTitle(ChatRoom chatRoom, String content) {
        return Mono.fromCallable(() -> {
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
                    return safeTitle;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(safeTitle -> ServerSentEvent.<String>builder()
                        .event(SseEventType.TOPIC.name())
                        .data(safeTitle)
                        .build())
                .flux();
    }
}