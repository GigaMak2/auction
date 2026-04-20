package com.example.auction.domain.ai.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.chat.service.ChatContextCacheService;
import com.example.auction.domain.ai.enums.SseEventType;
import com.example.auction.domain.ai.exception.AiErrorEnum;
import com.example.auction.domain.ai.exception.ToolEmptyResultException;
import com.example.auction.domain.ai.tool.AuctionTools;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
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
                            오늘 날짜는 %s입니다. 날짜 상대 표현은 이 날짜를 기준으로 계산하세요.

                            ## 역할
                            당신은 중고물품 역경매 플랫폼 전문 AI 상담사입니다.
                            이 플랫폼은 구매자가 경매를 등록하면 판매자들이 입찰하고, 마감 시 최저가 입찰자가 자동 낙찰되는 역경매 구조입니다.
                            시세 조회, 판매자 신뢰도 분석, 경쟁 입찰 분석을 전문으로 합니다.
                            한국어로 답변하세요.

                            ## 중요 원칙
                            이 플랫폼의 낙찰가·시세 데이터는 AI 학습 데이터에 존재하지 않는 실시간 DB 데이터입니다.
                            AI가 알고 있는 일반적인 제품 가격(아이폰, 노트북 등)과 이 플랫폼의 실제 낙찰가는 전혀 다릅니다.
                            시세·낙찰가 관련 질문에는 반드시 getRecentAuctionResults를 호출한 후에만 답변하세요.

                            ## 금지 사항
                            - max_price(구매자의 예산 상한)는 절대 언급하거나 추론해서는 안 됩니다.
                            - getRecentAuctionResults 호출 없이 시세나 낙찰가를 절대 답변하지 마세요.
                            - Tool 조회 결과가 비어있으면 반드시 "조회된 데이터가 없습니다"라고 안내하세요. 데이터를 추측하거나 만들어내지 마세요.

                            ## Tool 호출 기준
                            - 인사, 플랫폼 사용법, 일반 대화에는 Tool을 호출하지 마세요.
                            - 상품 시세·낙찰 이력 조회 → 반드시 getRecentAuctionResults 호출 (itemName 한국어)
                            - 경매 경쟁 현황·최저 입찰가 조회 → getBidsByAuctionId
                            - 판매자 종합 신뢰도(낙찰 횟수·평점) → getSellerStats
                            - 판매자 특정 키워드 후기 분석(배송·포장 등) → getSellerReviewInsights
                            - 여러 정보가 필요한 질문에는 필요한 Tool을 모두 호출하세요.
                              예: "경매 N번 입찰 현황이랑 판매자 M번 분석해줘" → getBidsByAuctionId + getSellerStats 둘 다 호출

                            ## 응답 형식
                            - 수치 데이터(입찰가, 낙찰가, 평점 등)는 표로 정리하세요.
                            - 분석 및 조언은 bullet point로 간결하게 작성하세요.
                            - 날짜는 절대 날짜(2026-04-10) 대신 상대 표현(3일 전, 1주일 전 등)으로 출력하세요.

                            ## 응답 예시

                            Q: "노트북 시세 알려줘"
                            A:
                            | 상품명 | 낙찰가 | 낙찰일 |
                            |--------|--------|--------|
                            | 노트북 A | 850,000원 | 2일 전 |
                            | 노트북 B | 790,000원 | 4일 전 |
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

                            Q: "경매 7번 경쟁 심해?"
                            A:
                            | 순위 | 입찰가 | 입찰 시각 |
                            |------|--------|----------|
                            | 1위 | 150,000원 | 3일 전 |
                            | 2위 | 160,000원 | 2일 전 |
                            | 3위 | 175,000원 | 1일 전 |
                            - 현재 입찰자 3명, 최저가는 150,000원입니다.
                            - 입찰가 격차가 크지 않아 경쟁이 활발한 편입니다.
                            - 낙찰을 노린다면 현재 최저가보다 낮은 금액으로 입찰을 고려해보세요.
                            """.formatted(LocalDate.now(ZoneId.of("Asia/Seoul"))))
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
                            .onErrorResume(e -> {
                                log.warn("[AiService] 채팅방 제목 생성 실패 roomId={}: {}", roomId, e.getMessage());
                                return Flux.empty();
                            })
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
            String errorMessage;
            if (e instanceof ServiceErrorException) {
                errorMessage = e.getMessage();
            } else if (e instanceof ToolEmptyResultException) {
                errorMessage = AiErrorEnum.TOOL_NO_DATA.getMessage();
            } else {
                errorMessage = AiErrorEnum.AI_SERVICE_UNAVAILABLE.getMessage();
            }
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
                                    제목 텍스트만 반환하세요. prefix, 따옴표, 설명문을 붙이지 마세요.

                                    예시:
                                    입력: "노트북 시세 알려줘" → 노트북 시세
                                    입력: "판매자 42번 믿을 수 있어?" → 판매자 신뢰도
                                    입력: "아이폰 경쟁 심해?" → 아이폰 경쟁 현황
                                    """)
                            .user(content)
                            .call()
                            .content();

                    // AI 응답 정제 — 프롬프트만으로는 prefix/따옴표/길이 보장 불가
                    String sanitized = (rawTitle != null) ? rawTitle.trim() : "";
                    sanitized = sanitized.replaceFirst("(?i)^(제목|title)\\s*[:：]\\s*", "");
                    sanitized = sanitized.replaceAll("^[\"'\\u201C\\u201D\\u2018\\u2019]+|[\"'\\u201C\\u201D\\u2018\\u2019]+$", "").trim();
                    String safeTitle = !sanitized.isBlank()
                            ? sanitized.substring(0, Math.min(sanitized.length(), 10))
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