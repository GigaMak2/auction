package com.example.auction.domain.auction.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.auction.dto.AuctionAdminSearchCondition;
import com.example.auction.domain.auction.dto.GetManyAuctionsResponse;
import com.example.auction.domain.auction.service.AuctionAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auctions")
public class AuctionAdminController {

    private final AuctionAdminService auctionAdminService;

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<GetManyAuctionsResponse>>> getAuctionList(
            @Valid @ModelAttribute AuctionAdminSearchCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "경매 목록 조회 요청 성공", auctionAdminService.getAuctionList(condition)));
    }

    @DeleteMapping("/{auctionId}")
    public ResponseEntity<BaseResponse<Void>> forceCancel(
            @PathVariable Long auctionId
    ) {
        auctionAdminService.forceCancel(auctionId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "경매 강제 취소 요청 성공", null));
    }
}
