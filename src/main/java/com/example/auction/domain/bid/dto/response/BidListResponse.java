package com.example.auction.domain.bid.dto.response;

import com.example.auction.domain.bid.entity.Bid;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

// 경매별 입찰 조회
@Getter
public class BidListResponse {

    private Long bidId;
    private Long auctionId;
    private Long price;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    public static BidListResponse of(Bid bid) {
        BidListResponse bidResponse = new BidListResponse();
        bidResponse.bidId = bid.getId();
        bidResponse.auctionId = bid.getAuctionId();
        bidResponse.price = bid.getPrice();
        bidResponse.createdAt = bid.getCreatedAt();

        return bidResponse;
    }

}
