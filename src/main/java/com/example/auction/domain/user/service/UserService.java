package com.example.auction.domain.user.service;

import com.example.auction.common.config.security.JwtProvider;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.user.dto.UserLoginRequest;
import com.example.auction.domain.user.dto.UserLoginResponse;
import com.example.auction.domain.user.dto.UserSignupRequest;
import com.example.auction.domain.user.dto.UserSignupResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    @Value("${jwt.refreshExpire}")
    private long refreshTokenExpireTime;

    @Transactional
    public UserSignupResponse signup(UserSignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ServiceErrorException(UserErrorEnum.DUPLICATED_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.of(request.email(), encodedPassword, request.role());
        userRepository.save(user);

        return new UserSignupResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }

    @Transactional
    public UserLoginResponse login(UserLoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ServiceErrorException(UserErrorEnum.INVALID_PASSWORD);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken();

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                refreshTokenExpireTime,
                TimeUnit.MILLISECONDS
        );

        return new UserLoginResponse(accessToken, refreshToken);
    }
}
