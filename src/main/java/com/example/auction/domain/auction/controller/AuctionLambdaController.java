package com.example.auction.domain.auction.controller;

import com.example.auction.domain.auction.service.AuctionLambdaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lambda에서 호출하는 내부 API
 * EventBridge Scheduler -> Lambda -> 이 api 호출
 */
@RestController
@RequestMapping("/lambda/auctions")
@RequiredArgsConstructor
public class AuctionLambdaController {

    private final AuctionLambdaService auctionLambdaService;

    // 경매 시작 (READY -> ACTIVE)
    @PostMapping("/{auctionId}/start")
    public ResponseEntity<Void> startAuction(@PathVariable Long auctionId) {
        auctionLambdaService.startAuction(auctionId);
        return ResponseEntity.ok().build();
    }

    // 경매 종료 (ACTIVE -> DONE/NO_BID)
    @PostMapping("/{auctionId}/end")
    public ResponseEntity<Void> endAuction(@PathVariable Long auctionId) {
        auctionLambdaService.endAuction(auctionId);
        return ResponseEntity.ok().build();
    }
}