package com.example.auction.domain.auction.search.service;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.domain.auction.dto.request.CreateAuctionRequest;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.search.document.AuctionDocument;
import com.example.auction.domain.auction.search.entity.AuctionDocumentReindexJob;
import com.example.auction.domain.auction.search.enums.AuctionDocumentReindexJobStatus;
import com.example.auction.domain.auction.search.repository.AuctionDocumentReindexJobRepository;
import com.example.auction.domain.auction.search.repository.AuctionDocumentRepository;
import com.example.auction.domain.auction.service.AuctionService;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.testutils.BaseIntegrationTest;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AuctionDocumentReindexServiceHelperTest extends BaseIntegrationTest {
    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuctionDocumentRepository auctionDocumentRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private AuctionDocumentReindexService auctionDocumentReindexService;

    @Autowired
    AuctionDocumentReindexJobRepository jobRepository;

    @Autowired
    private ElasticsearchOperations elasticsearch;

    private Long FAKE_USER_ID = 1L;
    private Long FAKE_CATEGORY_ID = 1L;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void setUp() throws Exception {
        flyway.clean();
        flyway.migrate();

        User user = userRepository.save(User.of("test@test.com", "encoded-password"));
        FAKE_USER_ID = user.getId();

        FAKE_CATEGORY_ID = categoryRepository.save(Category.root("FAKE")).getId();
    }

    @Test
    @DisplayName("elasticsearch reindex 테스트")
    void reindexTest() {
        // GIVEN
        for (int i=0; i<10; i++) {
            CreateAuctionRequest request = new CreateAuctionRequest();

            request.setItemName("경매 " + i);
            request.setMaxPrice(BigDecimal.valueOf(1000L));
            request.setCategoryId(FAKE_CATEGORY_ID);
            request.setStartedAt(LocalDateTime.now().plusDays(1));
            request.setEndedAt(LocalDateTime.now().plusDays(2));

            CustomUserDetails details = new CustomUserDetails(FAKE_USER_ID, UserRole.USER.name());

            auctionService.createAuction(details, request);
        }
        auctionDocumentRepository.deleteAll();

        // WHEN
        auctionDocumentReindexService.reindexAuctionDocuments();
        elasticsearch.indexOps(AuctionDocument.class).refresh();

        // THEN
        Iterable<AuctionDocument> auctionDocuments =  auctionDocumentRepository.findAll();

        List<Long> documentIds = new ArrayList<>();

        for (AuctionDocument document : auctionDocuments) {
            documentIds.add(document.getId());
        }

        List<Auction> auctions = auctionRepository.findAll();

        assertThat(documentIds.size()).isEqualTo(auctions.size());

        for (Auction auction : auctions) {
            assertThat(documentIds).contains(auction.getId());
        }

        List<AuctionDocumentReindexJob> jobs = jobRepository.findAll();
        assertThat(jobs.size()).isEqualTo(1);
        AuctionDocumentReindexJob job = jobs.getFirst();
        assertThat(job.getJobStatus()).isEqualTo(AuctionDocumentReindexJobStatus.DONE);
    }
}
