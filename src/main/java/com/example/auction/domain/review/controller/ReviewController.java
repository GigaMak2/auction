package com.example.auction.domain.review.controller;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.review.dto.*;
import com.example.auction.domain.review.service.ReviewImageService;
import com.example.auction.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewImageService reviewImageService;

    // S3 직접 업로드용 Presigned URL 발급 — 리뷰 작성 전 이미지 업로드 시 사용
    @GetMapping("/image/presigned-url")
    public ResponseEntity<BaseResponse<ReviewImagePresignResponse>> getPresignedUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "image/jpeg") String contentType
    ) {
        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.name(), "이미지 업로드 URL을 발급했습니다",
                reviewImageService.generatePresignedUrl(userDetails.getUserId(), contentType)));
    }

    @PostMapping
    public ResponseEntity<BaseResponse<ReviewCreateResponse>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "리뷰를 생성했습니다", reviewService.createReview(userId, request)));
    }

    @GetMapping("/written")
    public ResponseEntity<BaseResponse<PageResponse<ReviewListGetResponse>>> getWrittenReviewList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ReviewSearchCondition condition
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "작성한 리뷰 목록을 조회했습니다", reviewService.getWrittenReviewList(userId, condition)));
    }

    @GetMapping("/received")
    public ResponseEntity<BaseResponse<PageResponse<ReviewListGetResponse>>> getReceivedReviewList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ReviewSearchCondition condition
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "받은 리뷰 목록을 조회했습니다", reviewService.getReceivedReviewList(userId, condition)));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<BaseResponse<ReviewGetResponse>> getReview(
            @PathVariable Long reviewId
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "리뷰를 조회했습니다", reviewService.getReview(reviewId)));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<BaseResponse<ReviewModifyResponse>> modifyReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewModifyRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "리뷰를 수정했습니다", reviewService.modifyReview(userId, reviewId, request)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<BaseResponse<Void>> deleteReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reviewId
    ) {
        Long userId = userDetails.getUserId();
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "리뷰를 삭제했습니다", null));
    }
}
