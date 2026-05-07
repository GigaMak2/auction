package com.example.auction.domain.review.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.review.dto.ReviewAdminListResponse;
import com.example.auction.domain.review.dto.ReviewAdminSearchCondition;
import com.example.auction.domain.review.service.ReviewAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
public class ReviewAdminController {

    private final ReviewAdminService reviewAdminService;

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<ReviewAdminListResponse>>> getReviewList(
            @Valid @ModelAttribute ReviewAdminSearchCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "리뷰 목록 조회 요청 성공", reviewAdminService.getReviewList(condition)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<BaseResponse<Void>> forceDelete(
            @PathVariable Long reviewId
    ) {
        log.info("[ReviewAdminController] forceDelete — reviewId={}", reviewId); // 어드민 리뷰 강제 삭제 추적용
        reviewAdminService.forceDelete(reviewId);
        return ResponseEntity.ok(BaseResponse.success(HttpStatus.OK.name(), "리뷰 강제 삭제 요청 성공", null));
    }
}
