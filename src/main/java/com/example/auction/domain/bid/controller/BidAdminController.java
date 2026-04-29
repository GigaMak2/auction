package com.example.auction.domain.bid.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.bid.dto.request.BidAdminSearchCondition;
import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.service.BidAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class BidAdminController {

    private final BidAdminService bidAdminService;

    @GetMapping("/bids")
    public ResponseEntity<BaseResponse<PageResponse<BidAdminListResponse>>> getBidList(
            @Valid @ModelAttribute BidAdminSearchCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "입찰 목록 조회 요청 성공", bidAdminService.getBidList(condition)));
    }
}
