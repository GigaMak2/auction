package com.example.auction.domain.user.repository;

import com.example.auction.common.config.JpaConfig;
import com.example.auction.common.config.QuerydslConfig;
import com.example.auction.domain.auction.search.util.KoreanAnalyzerUtil;
import com.example.auction.domain.category.service.CategoryService;
import com.example.auction.domain.user.dto.UserListGetResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.testutils.BaseIntegrationTest;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
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
@Import({QuerydslConfig.class, JpaConfig.class, CategoryService.class, KoreanAnalyzerUtil.class, KoreanAnalyzer.class})
class UserCustomRepositoryImplTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(User.of("user1@test.com", "password"));
        userRepository.save(User.ofAdmin("admin@test.com", "password"));
        User deletedUser = User.of("user2@test.com", "password");
        deletedUser.delete();
        userRepository.save(deletedUser);
    }


    // ========================
    // 관리자 유저 목록 조회
    // ========================

    @Test
    @DisplayName("관리자 유저 목록 조회 - 필터 없음 전체 조회")
    void findUsersWithConditions_noFilter() {
        Page<UserListGetResponse> result = userRepository.findUsersWithConditions(
                PageRequest.of(0, 10), null, null, null);

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("관리자 유저 목록 조회 - 활성 유저만 필터링")
    void findUsersWithConditions_deletedFalse() {
        Page<UserListGetResponse> result = userRepository.findUsersWithConditions(
                PageRequest.of(0, 10), false, null, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(UserListGetResponse::deleted)
                .containsOnly(false);
    }

    @Test
    @DisplayName("관리자 유저 목록 조회 - 탈퇴 유저만 필터링")
    void findUsersWithConditions_deletedTrue() {
        Page<UserListGetResponse> result = userRepository.findUsersWithConditions(
                PageRequest.of(0, 10), true, null, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().deleted()).isTrue();
        assertThat(result.getContent().getFirst().email()).isEqualTo("user2@test.com");
    }

    @Test
    @DisplayName("관리자 유저 목록 조회 - 역할 필터링")
    void findUsersWithConditions_roleFilter() {
        Page<UserListGetResponse> result = userRepository.findUsersWithConditions(
                PageRequest.of(0, 10), null, UserRole.ADMIN, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().role()).isEqualTo(UserRole.ADMIN);
        assertThat(result.getContent().getFirst().email()).isEqualTo("admin@test.com");
    }

    @Test
    @DisplayName("관리자 유저 목록 조회 - 이메일 검색 필터링")
    void findUsersWithConditions_emailFilter() {
        Page<UserListGetResponse> result = userRepository.findUsersWithConditions(
                PageRequest.of(0, 10), null, null, "user");

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(UserListGetResponse::email)
                .allMatch(email -> email.contains("user"));
    }

    @Test
    @DisplayName("관리자 유저 목록 조회 - 조건에 맞는 결과 없음")
    void findUsersWithConditions_noMatch() {
        Page<UserListGetResponse> result = userRepository.findUsersWithConditions(
                PageRequest.of(0, 10), null, null, "없는이메일");

        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}