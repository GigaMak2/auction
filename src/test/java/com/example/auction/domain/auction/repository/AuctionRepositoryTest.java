package com.example.auction.domain.auction.repository;

import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.search.util.KoreanAnalyzerUtil;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import com.example.auction.domain.category.service.CategoryService;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.testutils.BaseIntegrationTest;

import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.flywaydb.core.Flyway;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.common.config.JpaConfig;
import org.springframework.test.util.ReflectionTestUtils;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
// H2좀 그만 불러!!
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaConfig.class, CategoryService.class, KoreanAnalyzerUtil.class, KoreanAnalyzer.class})
class AuctionRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long FAKE_USER_ID = 1L;
    private Long FAKE_CATEGORY_ID = 1L;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void setup() {
        flyway.clean();
        flyway.migrate();

        User user = userRepository.save(User.of("test@test.com", "encoded-password"));
        FAKE_USER_ID = user.getId();

        FAKE_CATEGORY_ID = categoryRepository.save(Category.root("FAKE")).getId();
    }

    /**
     * AuctionSearchCondition이 기본값일 때 모든 데이터를 가지고 오는지 확인합니다.
     */
    @Test
    void findSimple() {
        Auction auction = Auction.of(
            FAKE_USER_ID,
            "test auction",
            BigDecimal.valueOf(1000),
            "test auction item name",
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(2),
            FAKE_CATEGORY_ID
        );

        AuctionSearchCondition condition = new AuctionSearchCondition();

        auctionRepository.save(auction);

        List<@NonNull Auction> auctions = auctionRepository.findByCondition(
            condition
        ).stream().toList();

        assertThat(auctions).contains(auction);
    }

    /**
     * AuctionSearchCondition의 maxPriceMin과 maxPriceMax가 잘 작동하는 확인합니다.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getFindMinMaxPriceRangeSources")
    void findMinMaxPriceRange(String name, AuctionSearchCondition condition, int expectingAuction) {
        Function<BigDecimal, Auction> createAuction = (
            maxPrice
        ) -> {
            Auction auction = Auction.of(
                FAKE_USER_ID,
                "test auction",
                maxPrice,
                "test auction item name",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                FAKE_CATEGORY_ID
            );
            auctionRepository.save(auction);

            return auction;
        };

        // GIVEN
        Auction[] auctions = new Auction[] {
                createAuction.apply(BigDecimal.valueOf(1000)),
                createAuction.apply(BigDecimal.valueOf(2000)),
                createAuction.apply(BigDecimal.valueOf(3000))
        };

        // WHEN
        List<@NonNull Auction> foundAuctions = auctionRepository.findByCondition(
                condition
        ).stream().toList();

        // THEN
        assertThat(foundAuctions).containsExactly(auctions[expectingAuction]);
    }

    private static Stream<Arguments> getFindMinMaxPriceRangeSources() {
        AuctionSearchCondition cond1 = new AuctionSearchCondition();
        cond1.setMaxPriceMin(BigDecimal.valueOf(2500));

        AuctionSearchCondition cond2 = new AuctionSearchCondition();
        cond2.setMaxPriceMax(BigDecimal.valueOf(1500));

        AuctionSearchCondition cond3 = new AuctionSearchCondition();
        cond3.setMaxPriceMin(BigDecimal.valueOf(1500));
        cond3.setMaxPriceMax(BigDecimal.valueOf(2500));

        return Stream.of(
                Arguments.of("최소 가격 이상만 조회 가능", cond1, 2),
                Arguments.of("최대 가격 이하만 조회 가능", cond2, 0),
                Arguments.of("최소와 최대 가격 사이만 조회 가능", cond3, 1)
        );
    }

    /**
     * 경매의 상태로 필터링이 잘되는지 확인합니다
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getFindByStatusSources")
    void findByStatus(String name, AuctionSearchCondition condition, AuctionStatus[] statuses) {
        Function<AuctionStatus, Auction> createAuction = (
                status
        ) -> {
            Auction auction = Auction.of(
                    FAKE_USER_ID,
                    "test auction",
                    BigDecimal.valueOf(1000),
                    "test auction item name",
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(2),
                    FAKE_CATEGORY_ID
            );

            ReflectionTestUtils.setField(auction, "status", status);

            auctionRepository.save(auction);

            return auction;
        };

        // GIVEN
        createAuction.apply(AuctionStatus.READY);
        createAuction.apply(AuctionStatus.ACTIVE);
        createAuction.apply(AuctionStatus.DONE);
        createAuction.apply(AuctionStatus.NO_BID);
        createAuction.apply(AuctionStatus.CANCELLED);

        // WHEN
        List<AuctionStatus> foundAuctionStatuses = auctionRepository.findByCondition(
                condition
        ).stream()
            .map(a -> a.getStatus())
            .distinct()
            .toList();

        // THEN
        assertThat(foundAuctionStatuses).containsExactlyInAnyOrder(statuses);
    }

    private static Stream<Arguments> getFindByStatusSources() {
        AuctionSearchCondition cond1 = new AuctionSearchCondition();
        cond1.setDefaultStatusesIfEmpty(AuctionStatus.NO_BID);

        AuctionSearchCondition cond2 = new AuctionSearchCondition();
        cond2.setDefaultStatusesIfEmpty(AuctionStatus.ACTIVE, AuctionStatus.CANCELLED);

        return Stream.of(
                Arguments.of("경매 단일 조건 검색", cond1, new AuctionStatus[]{AuctionStatus.NO_BID}),
                Arguments.of("경매 다수 조건 검색", cond2, new AuctionStatus[]{AuctionStatus.ACTIVE, AuctionStatus.CANCELLED})
        );
    }



    // ========================
    // 관리자 경매 목록 조회
    // ========================

    @Test
    @DisplayName("관리자 경매 목록 조회 - 필터 없음 전체 조회")
    void findAuctionWithConditions_noFilter() {
        auctionRepository.save(Auction.of(FAKE_USER_ID, null, BigDecimal.valueOf(1000), "노트북",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), FAKE_CATEGORY_ID));
        Auction active = Auction.of(FAKE_USER_ID, null, BigDecimal.valueOf(2000), "키보드",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), FAKE_CATEGORY_ID);
        ReflectionTestUtils.setField(active, "status", AuctionStatus.ACTIVE);
        auctionRepository.save(active);

        Page<AuctionAdminListResponse> result = auctionRepository.findAuctionWithConditions(
                PageRequest.of(0, 10), null, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("관리자 경매 목록 조회 - 상태 필터링")
    void findAuctionWithConditions_statusFilter() {
        auctionRepository.save(Auction.of(FAKE_USER_ID, null, BigDecimal.valueOf(1000), "노트북",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), FAKE_CATEGORY_ID));
        Auction active = Auction.of(FAKE_USER_ID, null, BigDecimal.valueOf(2000), "키보드",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), FAKE_CATEGORY_ID);
        ReflectionTestUtils.setField(active, "status", AuctionStatus.ACTIVE);
        auctionRepository.save(active);

        Page<AuctionAdminListResponse> result = auctionRepository.findAuctionWithConditions(
                PageRequest.of(0, 10), AuctionStatus.ACTIVE, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("키보드");
        assertThat(result.getContent().get(0).status()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("관리자 경매 목록 조회 - 조건에 맞는 결과 없음")
    void findAuctionWithConditions_noMatch() {
        auctionRepository.save(Auction.of(FAKE_USER_ID, null, BigDecimal.valueOf(1000), "노트북",
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), FAKE_CATEGORY_ID));

        Page<AuctionAdminListResponse> result = auctionRepository.findAuctionWithConditions(
                PageRequest.of(0, 10), AuctionStatus.ACTIVE, null);

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}
