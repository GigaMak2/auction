package com.example.auction.domain.auth.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auth.dto.request.AuthAdminSignupRequest;
import com.example.auction.domain.auth.dto.response.AuthSignupResponse;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.enums.UserRole;
import com.example.auction.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthAdminServiceTest {

    @InjectMocks
    private AuthAdminService authAdminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private static final String VALID_SECRET_KEY = "test-secret-key";
    private static final String FAIL_KEY = "admin:signup:fail:admin@test.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authAdminService, "adminSecretKey", VALID_SECRET_KEY);
    }


    // ========================
    // 관리자 회원가입
    // ========================

    @Test
    @DisplayName("관리자 회원가입 성공")
    void signup_success() {
        // given
        AuthAdminSignupRequest request = new AuthAdminSignupRequest("admin@test.com", "password123", VALID_SECRET_KEY);
        User user = User.ofAdmin(request.email(), "encodedPassword");
        ReflectionTestUtils.setField(user, "id", 1L);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FAIL_KEY)).willReturn(null);
        given(userRepository.existsByEmail(request.email())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        AuthSignupResponse response = authAdminService.signup(request);

        // then
        assertThat(response.email()).isEqualTo("admin@test.com");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        verify(redisTemplate).delete(FAIL_KEY);
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 시크릿 키 불일치 시도 횟수 초과로 잠금")
    void signup_fail_locked() {
        // given
        AuthAdminSignupRequest request = new AuthAdminSignupRequest("admin@test.com", "password123", VALID_SECRET_KEY);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FAIL_KEY)).willReturn(5);

        // when & then
        assertThatThrownBy(() -> authAdminService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.ADMIN_SIGNUP_LOCKED.getMessage());

        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 시크릿 키 불일치")
    void signup_fail_invalidSecretKey() {
        // given
        AuthAdminSignupRequest request = new AuthAdminSignupRequest("admin@test.com", "password123", "wrong-secret-key");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FAIL_KEY)).willReturn(null);
        given(valueOperations.increment(FAIL_KEY)).willReturn(1L);

        // when & then
        assertThatThrownBy(() -> authAdminService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.INVALID_ADMIN_SECRET_KEY.getMessage());

        verify(valueOperations).increment(FAIL_KEY);
        verify(redisTemplate).expire(FAIL_KEY, 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 이메일 중복")
    void signup_fail_duplicatedEmail() {
        // given
        AuthAdminSignupRequest request = new AuthAdminSignupRequest("admin@test.com", "password123", VALID_SECRET_KEY);

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FAIL_KEY)).willReturn(null);
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authAdminService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthErrorEnum.DUPLICATED_EMAIL.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(redisTemplate).delete(FAIL_KEY);
    }
}