package com.example.auction.domain.bid.repository;

import com.example.auction.common.config.JpaConfig;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.search.util.KoreanAnalyzerUtil;
import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.service.CategoryService;
import com.example.auction.domain.category.repository.CategoryRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.testutils.BaseIntegrationTest;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.flywaydb.core.Flyway;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaConfig.class, CategoryService.class, KoreanAnalyzerUtil.class, KoreanAnalyzer.class})
class BidCustomRepositoryImplTest extends BaseIntegrationTest {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private Flyway flyway;

    private Long auctionId1;
    private Long auctionId2;

    private Long userId1;
    private Long userId2;

    @BeforeEach
    void setUp() {
        flyway.clean();
        flyway.migrate();

        User auctionOwner = userRepository.save(User.of("auction-owner@test.com", "1234qwer"));

        Long fakeCategoryId = categoryRepository.save(Category.root("FAKE")).getId();

        auctionId1 = auctionRepository.save(Auction.of(
            auctionOwner.getId(),
            null,
            BigDecimal.valueOf(1000),
            "auction1",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(2),
            fakeCategoryId
        )).getId();

        auctionId2 = auctionRepository.save(Auction.of(
                auctionOwner.getId(),
                null,
                BigDecimal.valueOf(2000),
                "auction2",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                fakeCategoryId
        )).getId();

        userId1 = userRepository.save(User.of("user1@test.com", "1234qwer")).getId();
        userId2 = userRepository.save(User.of("user2@test.com", "1234qwer")).getId();

        bidRepository.save(Bid.of("입찰1", java.math.BigDecimal.valueOf(1000), auctionId1, userId1, BidAuctionStatus.ACTIVE));
        bidRepository.save(Bid.of("입찰2", java.math.BigDecimal.valueOf(2000), auctionId1, userId2, BidAuctionStatus.ACTIVE));
        bidRepository.save(Bid.of("입찰3", java.math.BigDecimal.valueOf(3000), auctionId2, userId1, BidAuctionStatus.CLOSED));
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
                PageRequest.of(0, 10), null, auctionId1, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(BidAdminListResponse::auctionId)
                .containsOnly(auctionId1);
    }

    @Test
    @DisplayName("관리자 입찰 목록 조회 - userId 필터링")
    void findBidWithConditions_userIdFilter() {
        Page<BidAdminListResponse> result = bidRepository.findBidWithConditions(
                PageRequest.of(0, 10), null, null, userId1);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(BidAdminListResponse::userId)
                .containsOnly(userId1);
    }

    @Test
    @DisplayName("관리자 입찰 목록 조회 - 상태 + auctionId 복합 필터링")
    void findBidWithConditions_statusAndAuctionId() {
        Page<BidAdminListResponse> result = bidRepository.findBidWithConditions(
                PageRequest.of(0, 10), BidAuctionStatus.ACTIVE, auctionId1, null);

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
