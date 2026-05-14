package com.example.auction.domain.auction.util;

import java.time.LocalDateTime;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.request.AuctionSearchCondition;
import com.example.auction.domain.auction.dto.request.CreateAuctionRequest;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;

public class AuctionUtil {
    private AuctionUtil() {}

    public static void throwIfSearchConditionNotValid(
            AuctionSearchCondition condition
    ) {
        if (
                condition.getMaxPriceMin() != null &&
                condition.getMaxPriceMax() != null &&
                condition.getMaxPriceMax().compareTo(condition.getMaxPriceMin()) < 0
        ) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_SEARCH_INVALID_PRICE_RANGE);
        }
    }

    public static void throwIfCreateAuctionRequestNotValid(
            CreateAuctionRequest req
    ) {
        if (req.getMaxPrice().stripTrailingZeros().scale() > 0) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_CREATE_MAX_PRICE_NOT_WHOLE_NUMBER);
        }

        LocalDateTime now = LocalDateTime.now();

        if (req.getStartedAt().isBefore(now)) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_CREATE_STARTED_AT_IN_PAST);
        }

        if (req.getEndedAt().isBefore(now)) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_CREATE_ENDED_AT_IN_PAST);
        }

        if (req.getEndedAt().isBefore(req.getStartedAt())) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_CREATE_ENDED_AT_BEFORE_STARTED_AT);
        }

        if (req.getStartedAt().isBefore(now.plusMinutes(10))) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_CREATE_STARTED_AT_TOO_SOON);
        }
    }
}
