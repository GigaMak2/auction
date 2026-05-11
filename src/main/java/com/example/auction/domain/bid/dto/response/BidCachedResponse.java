package com.example.auction.domain.bid.dto.response;

import com.example.auction.domain.bid.entity.Bid;
import lombok.Getter;

import java.math.BigDecimal;

// BidResponse와 차이: UserId 가 있는 버전
// 유저들이 누가 입찰했는지는 몰라야 하기 때문에 기본 BidReponse에는 userId가 없고, 이 dto는 캐싱을 위해 존재하므로 입찰한 사람의 userId 존재
@Getter
public class BidCachedResponse {

    private Long bidId;
    private Long auctionId;
    private Long userId;
    private BigDecimal price;

    public static BidCachedResponse of(Bid bid) {
        BidCachedResponse response = new BidCachedResponse();
        response.bidId = bid.getId();
        response.auctionId = bid.getAuctionId();
        response.userId = bid.getUserId();
        response.price = bid.getPrice();
        return response;
    }
}
