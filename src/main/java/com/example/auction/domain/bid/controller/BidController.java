package com.example.auction.domain.bid.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.bid.dto.request.BidPageRequest;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.service.BidCommandFacade;
import com.example.auction.domain.bid.service.BidQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.auction.domain.bid.dto.response.BidResponse;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidCommandFacade commandService;
    private final BidQueryService queryService;

    @PostMapping("/v2")
    public ResponseEntity<BaseResponse<BidResponse>> placeBidDis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("auctionId") Long auctionId,
            @Valid @RequestBody BidRequest request
    ) {
        BidResponse data = commandService.placeBidDis(userDetails, auctionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(HttpStatus.CREATED.name(), "입찰을 생성했습니다", data));
    }

    @GetMapping("/v1")
    public ResponseEntity<BaseResponse<PageResponse<BidListResponse>>> getBids(
            @PathVariable("auctionId") Long auctionId,
            @ModelAttribute @Valid BidPageRequest request
            ) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.ASC, "price")
                        .and(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        PageResponse<BidListResponse> data = queryService.getBids(auctionId, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK.name(), "입찰 목록을 조회했습니다", data));
    }

    @GetMapping("/winner/v1")
    public ResponseEntity<BaseResponse<BidResponse>> getWinnerBid(
            @PathVariable("auctionId") Long auctionId
    ) {
        BidResponse data = queryService.getWinnerBid(auctionId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK.name(), "입찰 결과를 조회했습니다", data));
    }

    @GetMapping("/current/v1")
    public ResponseEntity<BaseResponse<BidResponse>> getCurrentMinBid(
            @PathVariable("auctionId") Long auctionId
    ) {
        BidResponse data = queryService.getCurrentMinBid(auctionId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK.name(), "현재 최저가 입찰을 조회했습니다", data));
    }
}
