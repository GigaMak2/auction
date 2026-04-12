package com.example.auction.domain.review.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.review.dto.*;
import com.example.auction.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<BaseResponse<ReviewCreateResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "리뷰 생성 요청 성공", reviewService.createReview(userId, request)));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<ReviewListGetResponse>>> getReviewList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute ReviewSearchCondition condition
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "리뷰 목록 조회 요청 성공", reviewService.getReviewList(userId, condition)));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<BaseResponse<ReviewGetResponse>> getReview(
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "리뷰 조회 요청 성공", reviewService.getReview(reviewId)));
    }

    @GetMapping("/{auctionId}")
    public ResponseEntity<BaseResponse<List<ReviewListByAuctionGetResponse>>> getReviewListByAuction(
            @PathVariable Long auctionId
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "경매별 리뷰 목록 조회 요청 성공", reviewService.getReviewListByAuction(auctionId)));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<BaseResponse<ReviewModifyResponse>> modifyReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewModifyRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "리뷰 수정 요청 성공", reviewService.modifyReview(userId, reviewId, request)));
    }
}
