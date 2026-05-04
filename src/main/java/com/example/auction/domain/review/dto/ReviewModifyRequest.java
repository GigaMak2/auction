package com.example.auction.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewModifyRequest(
        @Min(value = 1, message = "별점은 최소 1점입니다")
        @Max(value = 5, message = "별점은 최대 5점입니다")
        Integer score,

        @Size(max = 500, message = "리뷰 내용은 500자 이하로 입력해주세요")
        String description,

        // 이미지 URL 수정 — null이면 기존 값 유지, 새 URL이면 교체
        @Size(max = 512, message = "이미지 URL은 512자 이하로 입력해주세요")
        String imageUrl
) {}
