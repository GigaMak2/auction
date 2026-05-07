package com.example.auction.domain.userchat.listener;

import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import com.example.auction.domain.userchat.service.UserChatService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserChatRoomCreationListener implements MessageListener {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    
    private final AuctionResultRepository auctionResultRepository;
    private final UserChatService userChatService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        long auctionId;

        try {
            var tree = objectMapper.readTree(body);
            if (!"AUCTION_ENDED".equals(tree.path("eventType").asString())) {
                return;
            }
            auctionId = tree.path("auctionId").asLong();
        } catch (Exception e) {
            log.error("[UserChatRoom] 메시지 파싱 실패: body={}", body, e);
            return;
        }

        long finalAuctionId = auctionId;
        CompletableFuture.runAsync(() -> {
            try {
                auctionResultRepository.findByAuctionId(finalAuctionId).ifPresentOrElse(
                        result -> userChatService.createRoom(
                                result.getBuyerId(),
                                result.getSellerId(),
                                finalAuctionId
                        ),
                        () -> log.warn("[UserChatRoom] AuctionResult 없음 — auctionId={}", finalAuctionId)
                );
            } catch (Exception e) {
                log.error("[UserChatRoom] 채팅방 생성 실패 — auctionId={}", finalAuctionId, e);
            }
        }, executor);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
