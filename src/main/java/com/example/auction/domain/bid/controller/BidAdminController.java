package com.example.auction.domain.bid.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.bid.dto.request.BidAdminSearchCondition;
import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.service.BidAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/bids")
public class BidAdminController {

    private final BidAdminService bidAdminService;

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<BidAdminListResponse>>> getBidList(
            @Valid @ModelAttribute BidAdminSearchCondition condition
    ) {
        log.info("[BidAdminController] getBidList — status={}, auctionId={}, userId={}", condition.getStatus(), condition.getAuctionId(), condition.getUserId()); // 어드민 입찰 목록 조회 요청 추적용
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "입찰 목록 조회 요청 성공", bidAdminService.getBidList(condition)));
    }

    @DeleteMapping("/{bidId}")
    public ResponseEntity<BaseResponse<Void>> forceCancel(
            @PathVariable Long bidId
    ) {
        log.info("[BidAdminController] forceCancel — bidId={}", bidId); // 어드민 입찰 강제 취소 요청 추적용
        bidAdminService.forceCancel(bidId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "입찰 강제 취소 요청 성공", null));
    }
}
