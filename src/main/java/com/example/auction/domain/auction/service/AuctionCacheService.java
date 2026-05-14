package com.example.auction.domain.auction.service;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.auction.domain.auction.dto.request.AuctionSearchCondition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionCacheService {

    public boolean shouldCacheGetManyAuctionsPublic(
            AuctionSearchCondition condition
    ) {
        if (condition.getKeyword() != null) {
            return false;
        }

        if (!(condition.getMaxPriceMin() == null && condition.getMaxPriceMax() == null)) {
            return false;
        }

        if (condition.getPage() >= 9) {
            return false;
        }

        return true;
    }

    public String getManyAuctionsPublicCacheKey(
            AuctionSearchCondition condition
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("status=[");
        if (condition.getStatus() != null) {
            String statusKey = condition.getStatus()
                .stream().sorted().map(Enum::name).collect(Collectors.joining(","));
            builder.append(statusKey);
        }
        builder.append("]");

        builder.append("category=[");
        if (condition.getCategoryId() != null) {
            builder.append(condition.getCategoryId());
        }
        builder.append("]");

        builder.append("page=[");
        builder.append(condition.getPage());
        builder.append("]");

        builder.append("size=[");
        builder.append(condition.getSize());
        builder.append("]");

        return builder.toString();
    }
}
