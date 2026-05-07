package com.example.auction.domain.auction.repository;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.dto.CreateAuctionRequest;
import com.example.auction.domain.auction.dto.GetAuctionResponse;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.service.AuctionService;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.testutils.BaseIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
public class AuctionRepositorySearchTestSimple extends BaseIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuctionService auctionService;
    @Autowired
    private AuctionRepository auctionRepository;

    private Long FAKE_USER_ID = 1L;
    private Long FAKE_CATEGORY_ID = 1L;

    @Autowired
    private Flyway flyway;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setup() {
        flyway.clean();
        flyway.migrate();

        User user = userRepository.save(User.of("test@test.com", "encoded-password"));
        FAKE_USER_ID = user.getId();

        FAKE_CATEGORY_ID = categoryRepository.save(Category.root("FAKE")).getId();
    }

    @Test
    @DisplayName("관리자 경매 목록 조회 - 키워드 필터링")
    void findAuctionWithConditions_keywordFilter() {
        createAuction("노트북", null, BigDecimal.valueOf(1000), AuctionStatus.READY);
        createAuction("키보드", null, BigDecimal.valueOf(2000), AuctionStatus.READY);

        Page<AuctionAdminListResponse> result = auctionRepository.findAuctionWithConditions(
                PageRequest.of(0, 10), null, "노트");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("노트북");
    }

    @Test
    @DisplayName("관리자 경매 목록 조회 - 상태 + 키워드 동시 필터링")
    void findAuctionWithConditions_statusAndKeyword() {
        createAuction("노트북", null, BigDecimal.valueOf(1000), AuctionStatus.READY);
        createAuction("노트북 거치대", null, BigDecimal.valueOf(2000), AuctionStatus.ACTIVE);

        Page<AuctionAdminListResponse> result = auctionRepository.findAuctionWithConditions(
                PageRequest.of(0, 10), AuctionStatus.ACTIVE, "노트북");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(result.getContent().get(0).itemName()).isEqualTo("노트북 거치대");
    }

    @Test
    @DisplayName("경매 목록 조회 - 키워드")
    void findByCondition_keyword() {
        createAuction("노트북", null, BigDecimal.valueOf(1000), AuctionStatus.READY);
        createAuction("키보드", null, BigDecimal.valueOf(2000), AuctionStatus.READY);

        AuctionSearchCondition condition = new AuctionSearchCondition();
        condition.setKeyword("키보드");

        Page<Auction> result = auctionRepository.findByCondition(condition);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getItemName()).isEqualTo("키보드");
    }

    @Test
    @DisplayName("경매 목록 조회 - 키워드 순위")
    void findByCondition_keywordCheckRanking() {
        createAuction("노트북", "싼 노트북 입니다", BigDecimal.valueOf(1000), AuctionStatus.READY);
        createAuction("노트북", "비싼 노트북 입니다", BigDecimal.valueOf(2000), AuctionStatus.READY);

        AuctionSearchCondition condition = new AuctionSearchCondition();
        condition.setKeyword("비싼 노트북");

        Page<Auction> result = auctionRepository.findByCondition(condition);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("비싼 노트북 입니다");
    }

    void createAuction(
        String itemName,
        String description,
        BigDecimal maxPrice,
        AuctionStatus status
    ) {
        CreateAuctionRequest req = new CreateAuctionRequest();
        req.setItemName(itemName);
        req.setDescription(description);
        req.setMaxPrice(maxPrice);
        req.setStartedAt(LocalDateTime.now().plusDays(1));
        req.setEndedAt(LocalDateTime.now().plusDays(2));
        req.setCategoryId(FAKE_CATEGORY_ID);

        CustomUserDetails details = new CustomUserDetails(FAKE_USER_ID, UserRole.USER.name());
        GetAuctionResponse res = auctionService.createAuction(details, req);

        Auction auction = auctionRepository.findById(res.getId()).get();
        ReflectionTestUtils.setField(auction, "status", status);
        auctionRepository.save(auction);
    }
}
