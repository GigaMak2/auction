package com.example.auction.domain.auction.util;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.request.CreateAuctionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.auction.domain.auction.dto.request.AuctionSearchCondition;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionUtilTest {
    @Test
    @DisplayName("정상적인 경매 검색 요청은 실패를 하지 않습니다")
    void conditionNormal() {
        // GIVEN
        AuctionSearchCondition condition = new AuctionSearchCondition();

        // WHEN & THEN
        AuctionUtil.throwIfSearchConditionNotValid(condition);
    }

    @Test
    @DisplayName("경매 최소 가격이 최대 가격보다 클경우 에러가 납니다")
    void conditionInvalidMaxPriceRange() {
        // GIVEN
        AuctionSearchCondition condition = new AuctionSearchCondition();

        condition.setMaxPriceMax(BigDecimal.valueOf(1000));
        condition.setMaxPriceMin(BigDecimal.valueOf(2000));

        // WHEN & THEN
        assertThrows(ServiceErrorException.class, () -> {
            AuctionUtil.throwIfSearchConditionNotValid(condition);
        });
    }

    @Test
    @DisplayName("정상적인 경매 생성 요청은 통과")
    void noThrowForNormalCreation() {
        // GIVEN
        CreateAuctionRequest req = getNormalCreationRequest(LocalDateTime.now());

        // WHEN & THEN
        AuctionUtil.throwIfCreateAuctionRequestNotValid(req);
    }

    @Test
    @DisplayName("경매 생성 요청의 시작일은 과거여서는 안됩니다")
    void createRequestNoPastStart() {
        // GIVEN
        LocalDateTime now = LocalDateTime.now();
        CreateAuctionRequest req = getNormalCreationRequest(now);
        req.setStartedAt(now.minusSeconds(2));

        // WHEN & THEN
        assertThrows(ServiceErrorException.class, () -> {
            AuctionUtil.throwIfCreateAuctionRequestNotValid(req);
        });
    }

    @Test
    @DisplayName("경매 생성 요청의 종료일이 시작일보다 과거일 수는 없습니다")
    void createRequestNoEndBeforeStart() {
        // GIVEN
        LocalDateTime now = LocalDateTime.now();
        CreateAuctionRequest req = getNormalCreationRequest(now);
        req.setEndedAt(req.getStartedAt().minusSeconds(1));

        // WHEN & THEN
        assertThrows(ServiceErrorException.class, () -> {
            AuctionUtil.throwIfCreateAuctionRequestNotValid(req);
        });
    }

    @Test
    @DisplayName("경매 생성 요청의 시작시간은 현재 시간보다 10분 후여야 합니다")
    void createRequestThrowOnTooSoon() {
        // GIVEN
        LocalDateTime now = LocalDateTime.now();
        CreateAuctionRequest req = getNormalCreationRequest(now);
        req.setStartedAt(now.plusMinutes(5));

        // WHEN & THEN
        assertThrows(ServiceErrorException.class, () -> {
            AuctionUtil.throwIfCreateAuctionRequestNotValid(req);
        });
    }

    @Test
    @DisplayName("경매 생성 요청의 최대 가격은 정수여야 합니다")
    void createRequestMustBeWholeNumber() {
        // GIVEN
        CreateAuctionRequest req = getNormalCreationRequest(LocalDateTime.now());
        req.setMaxPrice(new BigDecimal("1000.2"));

        // WHEN & THEN
        assertThrows(ServiceErrorException.class, () -> {
            AuctionUtil.throwIfCreateAuctionRequestNotValid(req);
        });
    }

    private CreateAuctionRequest getNormalCreationRequest(LocalDateTime now) {
        CreateAuctionRequest req = new CreateAuctionRequest();
        req.setMaxPrice(new BigDecimal("1000"));
        req.setStartedAt(now.plusMinutes(20));
        req.setEndedAt(now.plusMinutes(30));

        return req;
    }
}
