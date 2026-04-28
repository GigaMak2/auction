package com.example.auction.domain.ai.listener;

import com.example.auction.domain.ai.service.AuctionEmbeddingService;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEmbedListener implements MessageListener {

    // embed 성공 후 설정 — 크래시 시에도 재시도 가능
    private static final String EMBED_DONE_KEY_PREFIX = "embed:done:auction:";
    // 처리 중 중복 요청 차단용 — 10분 TTL로 크래시 시 자동 해제
    private static final String EMBED_LOCK_KEY_PREFIX = "embed:lock:auction:";

    // JPA + OpenAI 블로킹 호출 전용 가상 스레드 Executor - Tomcat 요청 스레드와 격리
    private final ExecutorService embedExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final AuctionRepository auctionRepository;
    private final AuctionEmbeddingService auctionEmbeddingService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        Long auctionId;

        try {
            var tree = objectMapper.readTree(body);
            String eventType = tree.get("eventType").asText();
            if (!"AUCTION_ENDED".equals(eventType)) {
                return;
            }
            auctionId = tree.get("auctionId").asLong();
        } catch (Exception e) {
            log.error("[AuctionEmbed] 메시지 파싱 실패: body={}", body, e); // Redis Pub/Sub 메시지 형식 이상 감지 — body 포함으로 원인 추적 가능
            return;
        }

        Long finalAuctionId = auctionId;
        CompletableFuture.runAsync(() -> {
            try {
                var optionalAuction = auctionRepository.findById(finalAuctionId);
                if (optionalAuction.isEmpty()) {
                    log.warn("[AuctionEmbed] 경매 없음 — auctionId={}", finalAuctionId); // Lambda 이벤트는 왔는데 DB에 경매가 없는 데이터 정합성 이상 감지용
                    return;
                }
                var auction = optionalAuction.get();
                if (auction.getStatus() != AuctionStatus.DONE) {
                    return;
                }

                String doneKey = EMBED_DONE_KEY_PREFIX + finalAuctionId;
                String lockKey = EMBED_LOCK_KEY_PREFIX + finalAuctionId;

                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(doneKey))) {
                    return;
                }
                Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.MINUTES);
                if (Boolean.FALSE.equals(locked)) {
                    return;
                }
                try {
                    auctionEmbeddingService.embed(auction);
                    stringRedisTemplate.opsForValue().set(doneKey, "1", 30, TimeUnit.DAYS);
                    log.info("[AuctionEmbed] 임베딩 완료 — auctionId={}", finalAuctionId); // 낙찰 경매 임베딩 파이프라인 정상 완료 확인용 — RAG 검색 가능 상태 진입
                } catch (Exception e) {
                    log.error("[AuctionEmbed] 임베딩 처리 실패 — auctionId={}", finalAuctionId, e); // OpenAI 임베딩 API 실패 또는 pgvector 저장 실패 감지용
                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            } catch (Exception e) {
                log.error("[AuctionEmbed] 처리 중 예외 — auctionId={}", finalAuctionId, e); // 예상치 못한 예외 — 스택 트레이스 포함으로 원인 추적 가능
            }
        }, embedExecutor);
    }

    @PreDestroy
    public void shutdown() {
        embedExecutor.shutdown();
        try {
            if (!embedExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                embedExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            embedExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
