package com.example.auction.domain.user.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.repository.BidRepository;
import com.example.auction.domain.user.dto.UserDetailGetResponse;
import com.example.auction.domain.user.dto.UserListGetResponse;
import com.example.auction.domain.user.dto.UserSearchCondition;
import com.example.auction.domain.user.dto.UserWithdrawResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.entity.UserSocialAccount;
import com.example.auction.domain.user.enums.AuthProvider;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.user.repository.UserSocialAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @InjectMocks
    private UserAdminService userAdminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSocialAccountRepository userSocialAccountRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache auctionCache;


    // ========================
    // 유저 목록 조회
    // ========================

    @Test
    @DisplayName("유저 목록 조회 성공")
    void getUserList_success() {
        // given
        UserSearchCondition condition = new UserSearchCondition();
        LocalDateTime now = LocalDateTime.now();

        List<UserListGetResponse> userList = List.of(
                new UserListGetResponse(1L, "user1@test.com", UserRole.USER, false, now, null),
                new UserListGetResponse(2L, "user2@test.com", UserRole.USER, false, now, null)
        );
        Page<UserListGetResponse> page = new PageImpl<>(userList, PageRequest.of(0, 20), 2);

        given(userRepository.findUsersWithConditions(any(Pageable.class), any(), any(), any())).willReturn(page);

        // when
        PageResponse<UserListGetResponse> response = userAdminService.getUserList(condition);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("유저 목록 조회 성공 - 빈 리스트")
    void getUserList_success_empty() {
        // given
        UserSearchCondition condition = new UserSearchCondition();
        Page<UserListGetResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        given(userRepository.findUsersWithConditions(any(Pageable.class), any(), any(), any())).willReturn(page);

        // when
        PageResponse<UserListGetResponse> response = userAdminService.getUserList(condition);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }


    // ========================
    // 유저 상세 조회
    // ========================

    @Test
    @DisplayName("유저 상세 조회 성공 - 소셜 로그인 유저")
    void getUserDetail_success_socialUser() {
        // given
        Long userId = 1L;
        User user = User.of("user@test.com", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", userId);

        UserSocialAccount socialAccount = UserSocialAccount.of(userId, AuthProvider.KAKAO, "kakaoId");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userSocialAccountRepository.findByUserId(userId)).willReturn(Optional.of(socialAccount));

        // when
        UserDetailGetResponse response = userAdminService.getUserDetail(userId);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.provider()).isEqualTo(AuthProvider.KAKAO);
    }

    @Test
    @DisplayName("유저 상세 조회 성공 - 일반 유저 (소셜 계정 없음)")
    void getUserDetail_success_normalUser() {
        // given
        Long userId = 1L;
        User user = User.of("user@test.com", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userSocialAccountRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when
        UserDetailGetResponse response = userAdminService.getUserDetail(userId);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.provider()).isNull();
    }

    @Test
    @DisplayName("유저 상세 조회 실패 - 유저 없음")
    void getUserDetail_fail_userNotFound() {
        // given
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userAdminService.getUserDetail(userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.USER_NOT_FOUND.getMessage());
    }


    // ========================
    // 강제 회원 탈퇴
    // ========================

    @Test
    @DisplayName("강제 회원 탈퇴 성공 - 경매, 입찰 있는 경우")
    void forceWithdraw_success_withAuctionsAndBids() {
        // given
        Long userId = 1L;
        User user = User.of("user@test.com", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", userId);

        Auction auction = Auction.of(userId, null, BigDecimal.valueOf(10000), "상품", LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusDays(1), 1L);
        ReflectionTestUtils.setField(auction, "id", 10L);

        Bid auctionBid = Bid.of("경매 입찰", BigDecimal.valueOf(5000), 10L, 2L, BidAuctionStatus.ACTIVE);
        Bid userBid = Bid.of("내 입찰", BigDecimal.valueOf(3000), 20L, userId, BidAuctionStatus.ACTIVE);

        given(userRepository.findByIdAndDeletedFalse(userId)).willReturn(Optional.of(user));
        given(auctionRepository.findByUserIdAndStatusIn(eq(userId), any())).willReturn(List.of(auction));
        given(cacheManager.getCache("getAuction")).willReturn(auctionCache);
        given(bidRepository.findAllByAuctionIdAndStatus(10L, BidAuctionStatus.ACTIVE)).willReturn(List.of(auctionBid));
        given(bidRepository.findAllByUserIdAndStatus(userId, BidAuctionStatus.ACTIVE)).willReturn(List.of(userBid));

        // when
        UserWithdrawResponse response = userAdminService.forceWithdraw(userId);

        // then
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
        assertThat(auctionBid.getStatus()).isEqualTo(BidAuctionStatus.CANCELLED);
        assertThat(userBid.getStatus()).isEqualTo(BidAuctionStatus.CANCELLED);
        assertThat(response.email()).isEqualTo("user@test.com");
        verify(auctionCache).evict(10L);
        verify(redisTemplate).delete("refresh:" + userId);
    }

    @Test
    @DisplayName("강제 회원 탈퇴 성공 - 경매, 입찰 없는 경우")
    void forceWithdraw_success_noAuctionsAndBids() {
        // given
        Long userId = 1L;
        User user = User.of("user@test.com", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findByIdAndDeletedFalse(userId)).willReturn(Optional.of(user));
        given(auctionRepository.findByUserIdAndStatusIn(eq(userId), any())).willReturn(List.of());
        given(cacheManager.getCache("getAuction")).willReturn(auctionCache);
        given(bidRepository.findAllByUserIdAndStatus(userId, BidAuctionStatus.ACTIVE)).willReturn(List.of());

        // when
        UserWithdrawResponse response = userAdminService.forceWithdraw(userId);

        // then
        assertThat(response.email()).isEqualTo("user@test.com");
        assertThat(response.role()).isEqualTo(UserRole.USER);
        verify(redisTemplate).delete("refresh:" + userId);
    }

    @Test
    @DisplayName("강제 회원 탈퇴 실패 - 유저 없음")
    void forceWithdraw_fail_userNotFound() {
        // given
        Long userId = 1L;
        given(userRepository.findByIdAndDeletedFalse(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userAdminService.forceWithdraw(userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserErrorEnum.USER_NOT_FOUND.getMessage());
    }
}