package com.example.auction.domain.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// AI Tool이 LLM에 반환하는 입찰 정보 (getBidsByAuctionId 반환 타입)
public record AuctionBidInfo(
        BigDecimal price,      // 입찰가 (DB DECIMAL → BigDecimal)
        LocalDateTime bidTime  // 입찰 시각
) {}
