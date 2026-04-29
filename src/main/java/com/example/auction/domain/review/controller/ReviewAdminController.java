package com.example.auction.domain.review.controller;

import com.example.auction.common.dto.BaseResponse;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.review.dto.ReviewAdminListResponse;
import com.example.auction.domain.review.dto.ReviewAdminSearchCondition;
import com.example.auction.domain.review.service.ReviewAdminService;
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
}
