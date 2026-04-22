package com.example.auction.domain.ai.listener;

import com.example.auction.domain.ai.service.AuctionEmbeddingService;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEmbedListener implements MessageListener {

    private static final String EMBED_KEY_PREFIX = "embed:auction:";
    private static final ExecutorService embedExecutor = Executors.newFixedThreadPool(4);

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
            log.error("[AuctionEmbed] 메시지 파싱 실패: body={}, error={}", body, e.getMessage());
            return;
        }

        Long finalAuctionId = auctionId;
        // embed()는 JPA + OpenAI API 블로킹 호출 — 리스너 스레드 블로킹 방지를 위해 비동기 처리
        CompletableFuture.runAsync(() -> {
            try {
                var optionalAuction = auctionRepository.findById(finalAuctionId);
                if (optionalAuction.isEmpty()) {
                    log.warn("[AuctionEmbed] 경매 없음 — auctionId={}", finalAuctionId);
                    return;
                }
                var auction = optionalAuction.get();
                if (auction.getStatus() != AuctionStatus.DONE) {
                    log.debug("[AuctionEmbed] 낙찰 상태 아님, 스킵 — auctionId={}, status={}", finalAuctionId, auction.getStatus());
                    return;
                }
                String embedKey = EMBED_KEY_PREFIX + finalAuctionId;
                Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(embedKey, "1", 30, TimeUnit.DAYS);
                if (Boolean.FALSE.equals(isNew)) {
                    log.info("[AuctionEmbed] 이미 임베딩 처리됨, 스킵 — auctionId={}", finalAuctionId);
                    return;
                }
                try {
                    auctionEmbeddingService.embed(auction);
                    log.info("[AuctionEmbed] 임베딩 완료 — auctionId={}", finalAuctionId);
                } catch (Exception e) {
                    stringRedisTemplate.delete(embedKey);
                    throw e;
                }
            } catch (Exception e) {
                log.error("[AuctionEmbed] 임베딩 처리 실패 — auctionId={}, error={}", finalAuctionId, e.getMessage(), e);
            }
        }, embedExecutor); // ForkJoinPool.commonPool() 대신 전용 풀 — 블로킹 JPA/OpenAI 호출로 commonPool 고갈 방지
    }
}