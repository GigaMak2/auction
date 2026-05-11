package com.example.auction.domain.auction.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import com.example.auction.domain.auction.enums.AuctionStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuctionSearchCondition {
    private @Nullable String keyword;

    @PositiveOrZero(message = "최소 금액은 0 이상이어야 합니다")
    private @Nullable BigDecimal maxPriceMin;
    @Positive(message = "최대 금액은 0보다 커야합니다")
    private @Nullable BigDecimal maxPriceMax;

    private @Nullable Set<AuctionStatus> status;

    @Positive(message = "카테고리 아이디는 1 이상이어야 합니다")
    private @Nullable Long categoryId;

    @PositiveOrZero(message = "페이지는 0 이상이어야 합니다")
    private Integer page = 0;

    @Positive(message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private Integer size = 20;

    public void setDefaultStatusesIfEmpty(AuctionStatus... statuses) {
        if (this.status == null) {
            this.status = new HashSet<>();
        }

        if (this.status.isEmpty()) {
            this.status.addAll(Arrays.asList(statuses));
        }
    }

    public String toLogString() {
        return "keyword=\"%s\", maxPriceMin=%s, maxPriceMax=%s, status=%s, categoryId=%s, page=%s, size=%s"
            .formatted(this.keyword, this.maxPriceMin, this.maxPriceMax, this.status, this.categoryId, this.page, this.size);
    }
}
