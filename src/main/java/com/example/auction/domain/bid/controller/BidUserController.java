package com.example.auction.domain.bid.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.service.BidQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/bids")
@RequiredArgsConstructor
public class BidUserController {

    private final BidQueryService queryService;

    // 내 입찰 조회
    @GetMapping
    public ResponseEntity<BaseResponse<List<BidListResponse>>> getMyBids(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable("auction_id") Long auctionId
    ) {
        List<BidListResponse> data = queryService.getMyBids(authUser, auctionId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(String.valueOf(HttpStatus.OK.value()), "내 입찰 조회가 완료되었습니다.", data));
    }
}
