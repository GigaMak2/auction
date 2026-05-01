package com.example.auction.domain.bid.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.bid.dto.request.BidAdminSearchCondition;
import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.exceptions.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BidAdminServiceTest {

    @InjectMocks
    private BidAdminService bidAdminService;

    @Mock
    private BidRepository bidRepository;


    // ========================
    // 입찰 목록 조회
    // ========================

    @Test
    @DisplayName("입찰 목록 조회 성공")
    void getBidList_success() {
        // given
        BidAdminSearchCondition condition = new BidAdminSearchCondition();
        LocalDateTime now = LocalDateTime.now();

        List<BidAdminListResponse> bidList = List.of(
                new BidAdminListResponse(1L, 10L, 1L, BigDecimal.valueOf(1000), BidAuctionStatus.ACTIVE, now),
                new BidAdminListResponse(2L, 10L, 2L, BigDecimal.valueOf(900), BidAuctionStatus.CLOSED, now)
        );
        Page<BidAdminListResponse> page = new PageImpl<>(bidList, PageRequest.of(0, 20), 2);

        given(bidRepository.findBidWithConditions(any(Pageable.class), any(), any(), any())).willReturn(page);

        // when
        PageResponse<BidAdminListResponse> response = bidAdminService.getBidList(condition);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("입찰 목록 조회 성공 - 빈 리스트")
    void getBidList_success_empty() {
        // given
        BidAdminSearchCondition condition = new BidAdminSearchCondition();
        Page<BidAdminListResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        given(bidRepository.findBidWithConditions(any(Pageable.class), any(), any(), any())).willReturn(page);

        // when
        PageResponse<BidAdminListResponse> response = bidAdminService.getBidList(condition);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    // ========================
    // 입찰 강제 취소
    // ========================

    @Test
    @DisplayName("입찰 강제 취소 성공")
    void forceCancel_success() {
        // given
        Long bidId = 1L;
        Bid bid = Bid.of("입찰입니다", BigDecimal.valueOf(1000), 10L, 1L, BidAuctionStatus.ACTIVE);
        ReflectionTestUtils.setField(bid, "id", bidId);

        given(bidRepository.findById(bidId)).willReturn(Optional.of(bid));

        // when
        bidAdminService.forceCancel(bidId);

        // then
        assertThat(bid.getStatus()).isEqualTo(BidAuctionStatus.CANCELLED);
    }

    @Test
    @DisplayName("입찰 강제 취소 실패 - 입찰 없음")
    void forceCancel_fail_bidNotFound() {
        // given
        Long bidId = 1L;
        given(bidRepository.findById(bidId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bidAdminService.forceCancel(bidId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("입찰 강제 취소 실패 - 취소 불가 상태")
    void forceCancel_fail_notCancellable() {
        // given
        Long bidId = 1L;
        Bid bid = Bid.of("입찰입니다", BigDecimal.valueOf(1000), 10L, 1L, BidAuctionStatus.CLOSED);
        ReflectionTestUtils.setField(bid, "id", bidId);

        given(bidRepository.findById(bidId)).willReturn(Optional.of(bid));

        // when & then
        assertThatThrownBy(() -> bidAdminService.forceCancel(bidId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.BID_STATUS_NOT_CANCELLABLE.getMessage());
    }
}