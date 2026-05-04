package com.example.auction.domain.auction.search.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.example.auction.domain.auction.search.document.AuctionDocument;
import com.example.auction.domain.auction.search.dto.AuctionCreatedDocument;
import com.example.auction.domain.auction.search.dto.AuctionSearchResult;
import com.example.auction.domain.auction.search.repository.AuctionDocumentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.testutils.BaseIntegrationTest;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuctionElasticsearchServiceTest extends BaseIntegrationTest {
    // 가짜 유저 ID
    public static final Long FAKE_USER_ID = 1L;

    // 가짜 카테고리 ID
    public static final Long FAKE_CATEGORY_ID = 1L;

    @Autowired
    AuctionDocumentRepository auctionDocumentRepository;

    @Autowired
    AuctionElasticsearchService auctionElasticsearchService;

    @BeforeEach
    @AfterEach
    void clearElasticsearchIndex() {
        auctionDocumentRepository.deleteAll();
    }

    /**
     * AuctionSearchCondition이 기본값일 때 모든 데이터를 가지고 오는지 확인합니다.
     */
    @Test
    void findSimple() {
        AuctionCreatedDocument event = new AuctionCreatedDocument(
            1L,
            FAKE_USER_ID,
            "test auction item name",
            BigDecimal.valueOf(1000),
            "test auction",
            AuctionStatus.READY,
            LocalDateTime.now().plusDays(1),
            LocalDateTime.now().plusDays(2),
            null,
            FAKE_CATEGORY_ID,
            LocalDateTime.now()
        );

        AuctionDocument auctionDocument = AuctionDocument.from(event);

        AuctionSearchCondition condition = new AuctionSearchCondition();

        auctionDocumentRepository.save(auctionDocument);

        List<Long> resultIds = auctionElasticsearchService.searchAuctionFromElasticsearch(
            condition
        ).stream().map(AuctionSearchResult::id).toList();

        assertThat(resultIds).contains(1L);
    }

    /**
     * AuctionSearchCondition의 maxPriceMin과 maxPriceMax가 잘 작동하는 확인합니다.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("getFindMinMaxPriceRangeSources")
    void findMinMaxPriceRange(String name, AuctionSearchCondition condition, int expectingAuction) {
        long[] idCounter = {1};

        Function<BigDecimal, AuctionDocument> createAuction = (
            maxPrice
        ) -> {
            AuctionCreatedDocument event = new AuctionCreatedDocument(
                idCounter[0],
                FAKE_USER_ID,
                "test auction item name",
                maxPrice,
                "test auction",
                AuctionStatus.READY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
            );

            idCounter[0]++;

            return auctionDocumentRepository.save(AuctionDocument.from(event));
        };

        // GIVEN
        AuctionDocument[] auctions = new AuctionDocument[] {
                createAuction.apply(BigDecimal.valueOf(1000)),
                createAuction.apply(BigDecimal.valueOf(2000)),
                createAuction.apply(BigDecimal.valueOf(3000))
        };

        // WHEN
        List<Long> foundAuctions = auctionElasticsearchService.searchAuctionFromElasticsearch(
                condition
        ).stream().map(AuctionSearchResult::id).toList();

        // THEN
        assertThat(foundAuctions).containsExactly(auctions[expectingAuction].getId());
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
        long[] idCounter = {1};

        Function<AuctionStatus, AuctionDocument> createAuction = (
                status
        ) -> {
            AuctionCreatedDocument event = new AuctionCreatedDocument(
                    idCounter[0],
                    FAKE_USER_ID,
                    "test auction item name",
                    BigDecimal.valueOf(1000),
                    "test auction",
                    status,
                    LocalDateTime.now().plusDays(1),
                    LocalDateTime.now().plusDays(2),
                    null,
                    FAKE_CATEGORY_ID,
                    LocalDateTime.now()
            );

            idCounter[0]++;

            return auctionDocumentRepository.save(AuctionDocument.from(event));
        };

        // GIVEN
        createAuction.apply(AuctionStatus.READY);
        createAuction.apply(AuctionStatus.ACTIVE);
        createAuction.apply(AuctionStatus.DONE);
        createAuction.apply(AuctionStatus.NO_BID);
        createAuction.apply(AuctionStatus.CANCELLED);

        // WHEN
        List<AuctionStatus> foundAuctionStatuses = auctionElasticsearchService.searchAuctionFromElasticsearch(
                condition
        ).stream()
            .map(AuctionSearchResult::status)
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
        Long idCounter = 1L;

        AuctionCreatedDocument normal = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(1000),
                "노트북",
                AuctionStatus.READY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(normal));

        AuctionCreatedDocument active = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(2000),
                "키보드",
                AuctionStatus.ACTIVE,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(active));

        Page<AuctionAdminListResponse> result = auctionElasticsearchService.searchAuctionWithConditionsFromElasticsearch(
                PageRequest.of(0, 10), null, null
        );

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("관리자 경매 목록 조회 - 상태 필터링")
    void findAuctionWithConditions_statusFilter() {
        Long idCounter = 1L;

        AuctionCreatedDocument normal = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(1000),
                "노트북",
                AuctionStatus.READY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(normal));

        AuctionCreatedDocument active = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(2000),
                "키보드",
                AuctionStatus.ACTIVE,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(active));

        Page<AuctionAdminListResponse> result = auctionElasticsearchService.searchAuctionWithConditionsFromElasticsearch(
                PageRequest.of(0, 10), AuctionStatus.ACTIVE, null
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("키보드");
        assertThat(result.getContent().get(0).status()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("관리자 경매 목록 조회 - 키워드 필터링")
    void findAuctionWithConditions_keywordFilter() {
        Long idCounter = 1L;

        AuctionCreatedDocument normal = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(1000),
                "노트북",
                AuctionStatus.READY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(normal));

        AuctionCreatedDocument active = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(2000),
                "키보드",
                AuctionStatus.ACTIVE,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(active));

        Page<AuctionAdminListResponse> result = auctionElasticsearchService.searchAuctionWithConditionsFromElasticsearch(
                PageRequest.of(0, 10), null, "노트"
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("노트북");
    }

    @Test
    @DisplayName("관리자 경매 목록 조회 - 상태 + 키워드 동시 필터링")
    void findAuctionWithConditions_statusAndKeyword() {
        Long idCounter = 1L;

        AuctionCreatedDocument normal = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(1000),
                "노트북",
                AuctionStatus.READY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(normal));

        AuctionCreatedDocument active = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(2000),
                "노트북 거치대",
                AuctionStatus.ACTIVE,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(active));

        Page<AuctionAdminListResponse> result = auctionElasticsearchService.searchAuctionWithConditionsFromElasticsearch(
                PageRequest.of(0, 10), AuctionStatus.ACTIVE, "노트북"
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("노트북 거치대");
    }

    @Test
    @DisplayName("관리자 경매 목록 조회 - 조건에 맞는 결과 없음")
    void findAuctionWithConditions_noMatch() {
        Long idCounter = 1L;

        AuctionCreatedDocument normal = new AuctionCreatedDocument(
                idCounter++,
                FAKE_USER_ID,
                null,
                BigDecimal.valueOf(1000),
                "노트북",
                AuctionStatus.READY,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                null,
                FAKE_CATEGORY_ID,
                LocalDateTime.now()
        );
        auctionDocumentRepository.save(AuctionDocument.from(normal));

        Page<AuctionAdminListResponse> result = auctionElasticsearchService.searchAuctionWithConditionsFromElasticsearch(
                PageRequest.of(0, 10), AuctionStatus.ACTIVE, null
        );

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}
