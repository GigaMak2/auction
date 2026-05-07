package com.example.auction.domain.auth.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auth.dto.AuthAdminSignupRequest;
import com.example.auction.domain.auth.dto.AuthSignupResponse;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ADMIN_SIGNUP_FAIL_PREFIX = "admin:signup:fail:";
    private static final int MAX_ADMIN_SIGNUP_FAIL_COUNT = 5;
    private static final long ADMIN_SIGNUP_LOCK_DURATION_MINUTES = 5L;

    @Value("${admin.secret-key}")
    private String adminSecretKey;

    @Transactional
    public AuthSignupResponse signup(AuthAdminSignupRequest request) {
        String failKey = ADMIN_SIGNUP_FAIL_PREFIX + request.email();

        Number failCount = (Number) redisTemplate.opsForValue().get(failKey);
        if (failCount != null && failCount.intValue() >= MAX_ADMIN_SIGNUP_FAIL_COUNT) {
            throw new ServiceErrorException(AuthErrorEnum.ADMIN_SIGNUP_LOCKED);
        }

        if (!request.adminSecretKey().equals(adminSecretKey)) {
            incrementFailCount(failKey);
            log.warn("[AuthAdminService] 어드민 시크릿 키 검증 실패 — email={}", request.email()); // 어드민 계정 생성 시도 중 키 불일치 — 보안 위협 감지용
            throw new ServiceErrorException(AuthErrorEnum.INVALID_ADMIN_SECRET_KEY);
        }

        redisTemplate.delete(failKey);

        if (userRepository.existsByEmail(request.email())) {
            throw new ServiceErrorException(AuthErrorEnum.DUPLICATED_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.ofAdmin(request.email(), encodedPassword);
        userRepository.save(user);

        return new AuthSignupResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    private void incrementFailCount(String failKey) {
        Long count = redisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(failKey, ADMIN_SIGNUP_LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }
    }
}
