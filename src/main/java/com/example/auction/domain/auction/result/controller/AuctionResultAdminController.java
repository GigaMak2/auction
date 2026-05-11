package com.example.auction.domain.auction.result.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.auction.result.dto.response.AuctionResultAdminListResponse;
import com.example.auction.domain.auction.result.dto.request.AuctionResultAdminPageCondition;
import com.example.auction.domain.auction.result.service.AuctionResultAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auction-results")
public class AuctionResultAdminController {

    private final AuctionResultAdminService auctionResultAdminService;

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<AuctionResultAdminListResponse>>> getAuctionResultList(
            @Valid @ModelAttribute AuctionResultAdminPageCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "경매 결과 목록을 조회했습니다", auctionResultAdminService.getAuctionResultList(condition)));
    }
}
