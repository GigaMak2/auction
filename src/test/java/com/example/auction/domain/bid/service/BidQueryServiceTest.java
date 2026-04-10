package com.example.auction.domain.bid.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.bid.dto.response.BidListResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.enums.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class BidQueryServiceTest {

    @InjectMocks
    private BidQueryService queryService;

    @Mock
    private BidRepository bidRepository;

    // 현재 없으므로 임시로 id만 가지고 만듬
    private AuthUser authUser;
    private Long auctionId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        authUser = new AuthUser(1L);
        auctionId = 10L;
        pageable = PageRequest.of(0, 20);
    }


    // ========================
    // 경매별 입찰 목록 조회
    // ========================

    @Test
    @DisplayName("입찰 목록 조회 성공 - 입찰 2건 반환")
    void getBids_success() {
        // given
        List<Bid> bids = List.of(
                Bid.of(null, 100_000L, auctionId, 2L, BidAuctionStatus.ACTIVE),
                Bid.of(null, 150_000L, auctionId, 3L, BidAuctionStatus.ACTIVE)
        );
        Page<Bid> bidPage = new PageImpl<>(bids, pageable, bids.size());

        given(bidRepository.findAllByAuctionId(auctionId, pageable)).willReturn(bidPage);

        // when
        PageResponse<BidListResponse> response = queryService.getBids(authUser, auctionId, pageable);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.currentPage()).isEqualTo(0);
    }

    @Test
    @DisplayName("입찰 목록 조회 성공 - 입찰 없으면 빈 리스트 반환")
    void getBids_emptyList() {
        // given
        Page<Bid> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(bidRepository.findAllByAuctionId(auctionId, pageable)).willReturn(emptyPage);

        // when
        PageResponse<BidListResponse> response = queryService.getBids(authUser, auctionId, pageable);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }


    // ========================
    // 내 입찰 목록 조회
    // ========================
    @Test
    @DisplayName("내 입찰 목록 조회 성공")
    void getMyBids_success() {
        // given
        List<Bid> myBids = List.of(
                Bid.of(null, 100_000L, auctionId, authUser.getUserId(), BidAuctionStatus.ACTIVE),
                Bid.of(null, 80_000L, 20L, authUser.getUserId(), BidAuctionStatus.ACTIVE)
        );
        Page<Bid> myBidPage = new PageImpl<>(myBids, pageable, myBids.size());

        given(bidRepository.findAllByUserId(authUser.getUserId(), pageable)).willReturn(myBidPage);

        // when
        PageResponse<BidListResponse> response = queryService.getMyBids(authUser, pageable);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.content())
                .allMatch(bid -> bid.getAuctionId() != null);
    }

    @Test
    @DisplayName("내 입찰 없으면 빈 리스트 반환")
    void getMyBids_empty() {
        // given
        Page<Bid> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(bidRepository.findAllByUserId(authUser.getUserId(), pageable)).willReturn(emptyPage);

        // when
        PageResponse<BidListResponse> response = queryService.getMyBids(authUser, pageable);

        // then
        assertThat(response.content()).isEmpty();
    }

    // ========================
    // 입찰 결과 조회
    // ========================
    @Test
    @DisplayName("입찰 결과 조회 성공 - 최저가 입찰 반환")
    void getWinnerBid_success() {
        // given
        Bid winnerBid = Bid.of(null, 80_000L, auctionId, 2L, BidAuctionStatus.ACTIVE);
        given(bidRepository.findWinnerBidByAuctionId(auctionId)).willReturn(Optional.of(winnerBid));

        // when
        BidResponse response = queryService.getWinnerBid(authUser, auctionId);

        // then
        assertThat(response.getPrice()).isEqualTo(80_000L);
        assertThat(response.getAuctionId()).isEqualTo(auctionId);
    }

    @Test
    @DisplayName("입찰 없으면 예외 발생 - 미종료 또는 유찰 경매")
    void getWinnerBid_notFound() {
        // given
        given(bidRepository.findWinnerBidByAuctionId(auctionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queryService.getWinnerBid(authUser, auctionId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(BidErrorEnum.AUCTION_RESULT_NOT_FOUND.getMessage());

    }
}