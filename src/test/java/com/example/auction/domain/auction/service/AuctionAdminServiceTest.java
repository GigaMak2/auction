package com.example.auction.domain.auction.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.AuctionAdminSearchCondition;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.search.service.AuctionSearchService;

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
class AuctionAdminServiceTest {

    @InjectMocks
    private AuctionAdminService auctionAdminService;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionSearchService auctionSearchService;


    // ========================
    // 경매 목록 조회
    // ========================

    @Test
    @DisplayName("경매 목록 조회 성공")
    void getAuctionList_success() {
        // given
        AuctionAdminSearchCondition condition = new AuctionAdminSearchCondition();
        LocalDateTime now = LocalDateTime.now();

        List<AuctionAdminListResponse> auctionList = List.of(
                new AuctionAdminListResponse(1L, 1L, "상품1", 1L, AuctionStatus.ACTIVE, now, now, now.plusDays(1), null),
                new AuctionAdminListResponse(2L, 2L, "상품2", 2L, AuctionStatus.READY, now, now, now.plusDays(1), null),
                new AuctionAdminListResponse(3L, 3L, "상품3", 3L, AuctionStatus.CANCELLED, now, now, now.plusDays(1), now.plusMinutes(30))
        );
        Page<AuctionAdminListResponse> page = new PageImpl<>(auctionList, PageRequest.of(0,20), 3);

        given(auctionSearchService.searchAuctionWithConditions(any(Pageable.class), any(), any())).willReturn(page);

        // when
        PageResponse<AuctionAdminListResponse> response = auctionAdminService.getAuctionList(condition);

        // then
        assertThat(response.content()).hasSize(3);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("경매 목록 조회 성공 - 빈 리스트")
    void getAuctionList_success_empty() {
        // given
        AuctionAdminSearchCondition condition = new AuctionAdminSearchCondition();
        Page<AuctionAdminListResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        given(auctionSearchService.searchAuctionWithConditions(any(Pageable.class), any(), any())).willReturn(page);

        // when
        PageResponse<AuctionAdminListResponse> response = auctionAdminService.getAuctionList(condition);

        // then
        assertThat(response.content()).hasSize(0);
        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.isLast()).isTrue();
    }


    // ========================
    // 경매 강제 취소
    // ========================

    @Test
    @DisplayName("경매 강제 취소 성공 - READY 상태")
    void forceCancel_success_ready() {
        // given
        Long auctionId = 1L;
        Auction auction = Auction.of(1L, null, BigDecimal.valueOf(10000), "상품", LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusDays(1), 1L);
        ReflectionTestUtils.setField(auction, "id", auctionId);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

        // when
        auctionAdminService.forceCancel(auctionId);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
        assertThat(auction.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("경매 강제 취소 성공 - ACTIVE 상태")
    void forceCancel_success_active() {
        // given
        Long auctionId = 1L;
        Auction auction = Auction.of(1L, null, BigDecimal.valueOf(10000), "상품", LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusDays(1), 1L);
        ReflectionTestUtils.setField(auction, "id", auctionId);
        auction.activate();

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

        // when
        auctionAdminService.forceCancel(auctionId);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
        assertThat(auction.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("경매 강제 취소 실패 - 경매 없음")
    void forceCancel_fail_auctionNotFound() {
        // given
        Long auctionId = 1L;
        given(auctionRepository.findById(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> auctionAdminService.forceCancel(auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("경매 강제 취소 실패 - 취소 불가 상태")
    void forceCancel_fail_notCancellable() {
        // given
        Long auctionId = 1L;
        Auction auction = Auction.of(1L, null, BigDecimal.valueOf(10000), "상품", LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusDays(1), 1L);
        ReflectionTestUtils.setField(auction, "id", auctionId);
        ReflectionTestUtils.setField(auction, "status", AuctionStatus.DONE);

        given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));

        // when & then
        assertThatThrownBy(() -> auctionAdminService.forceCancel(auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuctionErrorEnum.AUCTION_STATUS_NOT_CANCELLABLE.getMessage());
    }
}
