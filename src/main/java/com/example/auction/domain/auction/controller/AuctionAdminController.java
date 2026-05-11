package com.example.auction.domain.auction.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.AuctionAdminSearchCondition;
import com.example.auction.domain.auction.service.AuctionAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auctions")
public class AuctionAdminController {

    private final AuctionAdminService auctionAdminService;

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<AuctionAdminListResponse>>> getAuctionList(
            @Valid @ModelAttribute AuctionAdminSearchCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "경매 목록을 조회했습니다", auctionAdminService.getAuctionList(condition)));
    }

    @DeleteMapping("/{auctionId}")
    public ResponseEntity<BaseResponse<Void>> forceCancel(
            @PathVariable Long auctionId
    ) {
        log.info("[AuctionAdminController] forceCancel — auctionId={}", auctionId); // 어드민 경매 강제 취소 추적용
        auctionAdminService.forceCancel(auctionId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "경매를 강제 취소했습니다", null));
    }
}
