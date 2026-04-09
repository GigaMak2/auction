package com.example.auction.domain.auction.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.domain.auction.dto.CreateAuctionRequest;
import com.example.auction.domain.auction.dto.GetAuctionResponse;
import com.example.auction.domain.auction.service.AuctionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor()
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping("/api/auctions/{auctionId}")
    public ResponseEntity<BaseResponse<GetAuctionResponse>> getAuction(@PathVariable Long auctionId) {
        GetAuctionResponse res = auctionService.getAuction(auctionId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(
                        String.valueOf(HttpStatus.OK.name()),
                        "경매 단건조회를 하였습니다",
                        res
                ));
    }

    @PostMapping("/api/auctions")
    public ResponseEntity<BaseResponse<GetAuctionResponse>> createAuction(
            @RequestBody @Valid CreateAuctionRequest req
    ) {
        // TODO: 실제 유저 객체가 생성된 이후 user id를 넣기
        GetAuctionResponse res = auctionService.createAuction(0L, req);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(
                        String.valueOf(HttpStatus.CREATED.name()),
                        "경매를 생성 하였습니다",
                        res
                ));
    }
}
