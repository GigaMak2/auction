package com.example.auction.domain.review.repository;

import com.example.auction.common.config.JpaConfig;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import com.example.auction.domain.category.service.CategoryService;
import com.example.auction.domain.review.dto.ReviewAdminListResponse;
import com.example.auction.domain.review.dto.ReviewListGetResponse;
import com.example.auction.domain.review.entity.Review;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.testutils.BaseIntegrationTest;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaConfig.class, CategoryService.class})
class ReviewCustomRepositoryImplTest extends BaseIntegrationTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private Flyway flyway;

    private Long user1Id = 1L;
    private Long user2Id = 2L;
    private Long user3Id = 3L;
    
    private Long user2Auction1Id = 1L;
    private Long user2Auction2Id = 1L;

    private Long user3AuctionId = 1L;

    @BeforeEach
    void setUp() {
        flyway.clean();
        flyway.migrate();

        Category category = categoryRepository.save(Category.root("FAKE"));

        User user1 = userRepository.save(User.of("user1@test.com", "password"));
        User user2 = userRepository.save(User.of("user2@test.com", "password"));
        User user3 = userRepository.save(User.of("user3@test.com", "password"));
        
        Auction user2Auction1 = createAuction(user2.getId(), category.getId());
        Auction user2Auction2 = createAuction(user2.getId(), category.getId());

        Auction user3Auction = createAuction(user3.getId(), category.getId());

        reviewRepository.save(Review.of(user2Auction1.getId(), user1.getId(), user2.getId(), 5, "좋아요", null));
        reviewRepository.save(Review.of(user2Auction2.getId(), user1.getId(), user3.getId(), 4, "괜찮아요", null));
        reviewRepository.save(Review.of(user3Auction.getId(), user3.getId(), user2.getId(), 3, "보통이에요", null));

        user1Id = user1.getId();
        user2Id = user2.getId();
        user3Id = user3.getId();

        user2Auction1Id = user2Auction1.getId();
        user2Auction2Id = user2Auction2.getId();

        user3AuctionId = user3Auction.getId();
    }

    // ========================
    // 작성한 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("작성한 리뷰 목록 조회 - 전체 조회")
    void findWrittenReviewsWithConditions_noFilter() {
        Page<ReviewListGetResponse> result = reviewRepository.findWrittenReviewsWithConditions(
                user1Id, PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 - 없으면 빈 결과")
    void findWrittenReviewsWithConditions_otherUser() {
        Page<ReviewListGetResponse> result = reviewRepository.findWrittenReviewsWithConditions(
                99L, PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("작성한 리뷰 목록 조회 - 날짜 범위 필터링")
    void findWrittenReviewsWithConditions_dateFilter() {
        Page<ReviewListGetResponse> inRange = reviewRepository.findWrittenReviewsWithConditions(
                user1Id, PageRequest.of(0, 10),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        Page<ReviewListGetResponse> outOfRange = reviewRepository.findWrittenReviewsWithConditions(
                user1Id, PageRequest.of(0, 10),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2)
        );

        assertThat(inRange.getContent()).hasSize(2);
        assertThat(outOfRange.getContent()).isEmpty();
    }


    // ========================
    // 받은 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("받은 리뷰 목록 조회 - 전체 조회")
    void findReceivedReviewsWithConditions_noFilter() {
        Page<ReviewListGetResponse> result = reviewRepository.findReceivedReviewsWithConditions(
                user2Id, PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 - 없으면 빈 결과")
    void findReceivedReviewsWithConditions_otherUser() {
        Page<ReviewListGetResponse> result = reviewRepository.findReceivedReviewsWithConditions(
                99L, PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("받은 리뷰 목록 조회 - 날짜 범위 필터링")
    void findReceivedReviewsWithConditions_dateFilter() {
        Page<ReviewListGetResponse> inRange = reviewRepository.findReceivedReviewsWithConditions(
                user2Id, PageRequest.of(0, 10),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        );

        Page<ReviewListGetResponse> outOfRange = reviewRepository.findReceivedReviewsWithConditions(
                user2Id, PageRequest.of(0, 10),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2)
        );

        assertThat(inRange.getContent()).hasSize(2);
        assertThat(outOfRange.getContent()).isEmpty();
    }


    // ========================
    // 관리자 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("관리자 리뷰 목록 조회 - 필터 없음 전체 조회")
    void findReviewsWithConditions_noFilter() {
        Page<ReviewAdminListResponse> result = reviewRepository.findReviewsWithConditions(
                PageRequest.of(0, 10), null, null, null, null);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("관리자 리뷰 목록 조회 - auctionId 필터링")
    void findReviewsWithConditions_auctionIdFilter() {
        Page<ReviewAdminListResponse> result = reviewRepository.findReviewsWithConditions(
                PageRequest.of(0, 10), user2Auction1Id, null, null, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().auctionId()).isEqualTo(user2Auction1Id);
    }

    @Test
    @DisplayName("관리자 리뷰 목록 조회 - reviewerId 필터링")
    void findReviewsWithConditions_reviewerIdFilter() {
        Page<ReviewAdminListResponse> result = reviewRepository.findReviewsWithConditions(
                PageRequest.of(0, 10), null, user1Id, null, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(ReviewAdminListResponse::reviewerId)
                .containsOnly(1L);
    }

    @Test
    @DisplayName("관리자 리뷰 목록 조회 - revieweeId 필터링")
    void findReviewsWithConditions_revieweeIdFilter() {
        Page<ReviewAdminListResponse> result = reviewRepository.findReviewsWithConditions(
                PageRequest.of(0, 10), null, null, user2Id, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(ReviewAdminListResponse::revieweeId)
                .containsOnly(2L);
    }

    @Test
    @DisplayName("관리자 리뷰 목록 조회 - score 필터링")
    void findReviewsWithConditions_scoreFilter() {
        Page<ReviewAdminListResponse> result = reviewRepository.findReviewsWithConditions(
                PageRequest.of(0, 10), null, null, null, 5);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().score()).isEqualTo(5);
    }

    @Test
    @DisplayName("관리자 리뷰 목록 조회 - reviewerId + score 복합 필터링")
    void findReviewsWithConditions_reviewerIdAndScore() {
        Page<ReviewAdminListResponse> result = reviewRepository.findReviewsWithConditions(
                PageRequest.of(0, 10), null, user1Id, null, 4);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().auctionId()).isEqualTo(user2Auction2Id);
    }

    @Test
    @DisplayName("관리자 리뷰 목록 조회 - 조건에 맞는 결과 없음")
    void findReviewsWithConditions_noMatch() {
        Page<ReviewAdminListResponse> result = reviewRepository.findReviewsWithConditions(
                PageRequest.of(0, 10), null, null, null, 1);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    private Auction createAuction(Long userId, Long categoryId) {
        Auction auction = Auction.of(
                userId,
                "테스트 경매",
                BigDecimal.valueOf(200_000),
                "테스트 경매",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                categoryId
        );
        return auctionRepository.save(auction);
    }
}
