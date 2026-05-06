package com.example.auction.domain.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotNull(message = "경매 식별자를 입력해주세요")
        Long auctionId,

        @Min(value = 1, message = "별점은 최소 1점입니다")
        @Max(value = 5, message = "별점은 최대 5점입니다")
        int score,

        @Size(max = 500, message = "리뷰 내용은 500자 이하로 입력해주세요")
        String description,

        // CloudFront 이미지 URL — 선택 입력, Presigned URL 발급 후 S3 업로드 완료된 URL
        @Size(max = 512, message = "이미지 URL은 512자 이하로 입력해주세요")
        String imageUrl
) {}
