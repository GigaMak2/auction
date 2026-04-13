package com.example.auction.domain.bid.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.exceptions.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BidCommandServiceTest {

    @InjectMocks
    private BidCommandService commandService;

    @Mock
    private BidRepository bidRepository;

    private CustomUserDetails userDetails;
    private Long auctionId;

    @BeforeEach
    void setUp() {
        userDetails = new CustomUserDetails(1L, "USER");  // 입찰자(판매자) userId = 1
        auctionId = 10L;
    }

    // ========================
    // 입찰 생성 성공 케이스
    // ========================
    @Test
    @DisplayName("첫 입찰 - 현재 입찰 없을 때 max_price 이하면 성공")
    void firstBid_success() {
        // given
        BidRequest request = new BidRequest(BigDecimal.valueOf(150_000), null);
        Bid savedBid = Bid.of(null, BigDecimal.valueOf(150_000), auctionId, userDetails.getUserId(), BidAuctionStatus.ACTIVE);

        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.empty());
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        // when
        BidResponse response = commandService.placeBid(userDetails, auctionId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getPrice()).isEqualTo(BigDecimal.valueOf(150_000));
        assertThat(response.getAuctionId()).isEqualTo(auctionId);
    }

    @Test
    @DisplayName("현재 최저가보다 낮은 가격으로 입찰 성공")
    void bidLowerThanCurrentMin_success() {
        // given
        BigDecimal currentMinPrice = BigDecimal.valueOf(150000);
        BigDecimal newBidPrice = BigDecimal.valueOf(100000);  // 현재 최저가보다 낮음
        BidRequest request = new BidRequest(newBidPrice, null);
        Bid savedBid = Bid.of(null, newBidPrice, auctionId, userDetails.getUserId(), BidAuctionStatus.ACTIVE);

        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.of(currentMinPrice));
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        // when
        BidResponse response = commandService.placeBid(userDetails, auctionId, request);

        // then
        assertThat(response.getPrice()).isEqualTo(newBidPrice);
    }


    // ========================
    // 입찰 생성 실패 케이스
    // ========================

    @Test
    @DisplayName("현재 최저가와 같은 가격으로 입찰 시 실패")
    void bidSameAsCurrentMin_fail() {
        // given
        BigDecimal currentMinPrice = BigDecimal.valueOf(150_000);
        BidRequest request = new BidRequest(currentMinPrice, null);

        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.of(currentMinPrice));

        // when & then
        assertThatThrownBy(() -> commandService.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_PRICE_NOT_LOWER.getMessage());
    }

    @Test
    @DisplayName("현재 최저가보다 높은 가격으로 입찰 시 실패")
    void bidHigherThanCurrentMin_fail() {
        // given
        BigDecimal currentMinPrice = BigDecimal.valueOf(150000);
        BidRequest request = new BidRequest(BigDecimal.valueOf(200_000), null);  // 최저가보다 높음

        given(bidRepository.findMinPriceByAuctionId(auctionId)).willReturn(Optional.of(currentMinPrice));

        // when & then
        assertThatThrownBy(() -> commandService.placeBid(userDetails, auctionId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_PRICE_NOT_LOWER.getMessage());
    }

}