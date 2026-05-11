package com.example.auction.domain.userchat.repository;

import com.example.auction.common.config.JpaConfig;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.search.util.KoreanAnalyzerUtil;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import com.example.auction.domain.category.service.CategoryService;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.userchat.entity.UserChatMessage;
import com.example.auction.domain.userchat.entity.UserChatRoom;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuerydslConfig.class, JpaConfig.class, CategoryService.class, KoreanAnalyzerUtil.class, KoreanAnalyzer.class})
class UserChatMessageCustomRepositoryImplTest extends BaseIntegrationTest {

    @Autowired
    private UserChatMessageRepository userChatMessageRepository;

    @Autowired
    private UserChatRoomRepository userChatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private Flyway flyway;

    private Long ROOM_ID;
    private Long SENDER_ID;

    @BeforeEach
    void setUp() {
        flyway.clean();
        flyway.migrate();

        // FK 체인: categories → users → auctions → user_chat_rooms → user_chat_messages
        Category category = categoryRepository.save(Category.root("테스트 카테고리"));

        User buyer  = userRepository.save(User.of("buyer@test.com",  "password"));
        User seller = userRepository.save(User.of("seller@test.com", "password"));
        SENDER_ID = buyer.getId();

        Auction auction = auctionRepository.save(Auction.of(
                seller.getId(), null,
                BigDecimal.valueOf(10000), "테스트 상품",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                category.getId()
        ));

        ROOM_ID = userChatRoomRepository.save(
                UserChatRoom.of(buyer.getId(), seller.getId(), auction.getId())
        ).getId();

        userChatMessageRepository.save(UserChatMessage.of(ROOM_ID, SENDER_ID, "메시지1"));
        userChatMessageRepository.save(UserChatMessage.of(ROOM_ID, SENDER_ID, "메시지2"));
        userChatMessageRepository.save(UserChatMessage.of(ROOM_ID, SENDER_ID, "메시지3"));
        userChatMessageRepository.save(UserChatMessage.of(ROOM_ID, SENDER_ID, "메시지4"));
        userChatMessageRepository.save(UserChatMessage.of(ROOM_ID, SENDER_ID, "메시지5"));
    }


    // ========================
    // findByCursor
    // ========================

    @Test
    @DisplayName("findByCursor - cursor 없음: 최신 N개 ASC 반환")
    void findByCursor_noCursor() {
        // when
        List<UserChatMessage> result = userChatMessageRepository.findByCursor(ROOM_ID, null, 3);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("메시지3");
        assertThat(result.get(1).getContent()).isEqualTo("메시지4");
        assertThat(result.get(2).getContent()).isEqualTo("메시지5");
    }

    @Test
    @DisplayName("findByCursor - cursor 있음: cursor id 이전 메시지 ASC 반환")
    void findByCursor_withCursor() {
        // given
        List<UserChatMessage> all = userChatMessageRepository.findAll(Sort.by("id").ascending());
        Long thirdId = all.get(2).getId();

        // when
        List<UserChatMessage> result = userChatMessageRepository.findByCursor(ROOM_ID, thirdId, 2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> m.getId() < thirdId);
        assertThat(result.get(0).getId()).isLessThan(result.get(1).getId());
    }

    @Test
    @DisplayName("findByCursor - 해당 채팅방 메시지 없음: 빈 리스트 반환")
    void findByCursor_emptyRoom() {
        // when
        List<UserChatMessage> result = userChatMessageRepository.findByCursor(999L, null, 20);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByCursor - size보다 메시지 수가 적으면 전체 반환")
    void findByCursor_lessThanSize() {
        // when
        List<UserChatMessage> result = userChatMessageRepository.findByCursor(ROOM_ID, null, 20);

        // then
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getContent()).isEqualTo("메시지1");
        assertThat(result.get(4).getContent()).isEqualTo("메시지5");
    }
}
