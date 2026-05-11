package com.example.auction.domain.bid.dto.response;

import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class BidResponse {

    private Long bidId;
    private Long auctionId;
    private BigDecimal price;
    private String description;
    private BidAuctionStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    public static BidResponse of(Bid bid) {
        BidResponse bidResponse = new BidResponse();
        bidResponse.bidId = bid.getId();
        bidResponse.auctionId = bid.getAuctionId();
        bidResponse.price = bid.getPrice();
        bidResponse.description = bid.getDescription();
        bidResponse.createdAt = bid.getCreatedAt();
        bidResponse.status = bid.getStatus();

        return bidResponse;
    }

    public static BidResponse fromCached(BidCachedResponse cached) {
        BidResponse bidResponse = new BidResponse();
        bidResponse.bidId = cached.getBidId();
        bidResponse.auctionId = cached.getAuctionId();
        bidResponse.price = cached.getPrice();
        bidResponse.description = cached.getDescription();
        bidResponse.status = cached.getStatus();
        bidResponse.createdAt = cached.getCreatedAt();
        return bidResponse;
    }
}
