package com.example.auction.domain.auction.util;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;

public class AuctionUtil {
    private AuctionUtil() {}

    public static void throwIfSearchConditionNotValid(
            AuctionSearchCondition condition
    ) {
        // 검색 조건중 최소금액이 최대 금액 보다 클경우 에러를 던지기
        if (
                condition.getMaxPriceMax() != null &&
                condition.getMaxPriceMax().compareTo(condition.getMaxPriceMin()) < 0
        ) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_SEARCH_INVLID_PRICE_RANGE);
        }
    }
}
