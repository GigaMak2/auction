package com.example.auction.domain.bid.repository;

import com.example.auction.common.config.JpaConfig;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.category.service.CategoryService;
import com.example.auction.testutils.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaConfig.class, CategoryService.class})
class BidCustomRepositoryImplTest extends BaseIntegrationTest {

    @Autowired
    private BidRepository bidRepository;

    @BeforeEach
    void setUp() {
        bidRepository.save(Bid.of("입찰1", java.math.BigDecimal.valueOf(1000), 10L, 1L, BidAuctionStatus.ACTIVE));
        bidRepository.save(Bid.of("입찰2", java.math.BigDecimal.valueOf(2000), 10L, 2L, BidAuctionStatus.ACTIVE));
        bidRepository.save(Bid.of("입찰3", java.math.BigDecimal.valueOf(3000), 20L, 1L, BidAuctionStatus.CLOSED));
    }


    // ========================
    // 관리자 입찰 목록 조회
    // ========================

    @Test
    @DisplayName("관리자 입찰 목록 조회 - 필터 없음 전체 조회")
    void findBidWithConditions_noFilter() {
        Page<BidAdminListResponse> result = bidRepository.findBidWithConditions(
                PageRequest.of(0, 10), null, null, null);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("관리자 입찰 목록 조회 - 상태 필터링")
    void findBidWithConditions_statusFilter() {
        Page<BidAdminListResponse> result = bidRepository.findBidWithConditions(
                PageRequest.of(0, 10), BidAuctionStatus.ACTIVE, null, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(BidAdminListResponse::status)
                .containsOnly(BidAuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("관리자 입찰 목록 조회 - auctionId 필터링")
    void findBidWithConditions_auctionIdFilter() {
        Page<BidAdminListResponse> result = bidRepository.findBidWithConditions(
                PageRequest.of(0, 10), null, 10L, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(BidAdminListResponse::auctionId)
                .containsOnly(10L);
    }

    @Test
    @DisplayName("관리자 입찰 목록 조회 - userId 필터링")
    void findBidWithConditions_userIdFilter() {
        Page<BidAdminListResponse> result = bidRepository.findBidWithConditions(
                PageRequest.of(0, 10), null, null, 1L);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(BidAdminListResponse::userId)
                .containsOnly(1L);
    }

    @Test
    @DisplayName("관리자 입찰 목록 조회 - 상태 + auctionId 복합 필터링")
    void findBidWithConditions_statusAndAuctionId() {
        Page<BidAdminListResponse> result = bidRepository.findBidWithConditions(
                PageRequest.of(0, 10), BidAuctionStatus.ACTIVE, 10L, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(BidAdminListResponse::status)
                .containsOnly(BidAuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("관리자 입찰 목록 조회 - 조건에 맞는 결과 없음")
    void findBidWithConditions_noMatch() {
        Page<BidAdminListResponse> result = bidRepository.findBidWithConditions(
                PageRequest.of(0, 10), BidAuctionStatus.CANCELLED, null, null);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}