package com.example.auction.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewAdminSearchCondition {

    @PositiveOrZero(message = "페이지는 0 이상이어야 합니다")
    private int page = 0;

    @Positive(message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private int size = 20;

    private Long auctionId;
    private Long reviewerId;
    private Long revieweeId;
    private Integer score;
}
