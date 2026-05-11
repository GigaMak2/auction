package com.example.auction.domain.bid.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.bid.dto.request.BidPageRequest;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
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


@RestController
@RequestMapping("/api/me/bids")
@RequiredArgsConstructor
public class BidUserController {

    private final BidQueryService queryService;

    // 내 입찰 조회
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<BidListResponse>>> getMyBids(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute @Valid BidPageRequest request
            ) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<BidListResponse> data = queryService.getMyBids(userDetails, pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.success(HttpStatus.OK.name(), "내 입찰 목록을 조회했습니다", data));
    }
}
