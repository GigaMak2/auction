package com.example.auction.domain.auction.eventBridge;

import java.time.LocalDateTime;

public record AuctionCancelledEventBridge (
        Long auctionId
) {}
