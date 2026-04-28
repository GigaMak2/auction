package com.example.auction.domain.auth.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auth.dto.AuthAdminSignupRequest;
import com.example.auction.domain.auth.dto.AuthSignupResponse;
import com.example.auction.domain.auth.exception.AuthErrorEnum;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.secret-key}")
    private String adminSecretKey;

    public AuthSignupResponse signup(AuthAdminSignupRequest request) {
        if (!request.adminSecretKey().equals(adminSecretKey)) {
            throw new ServiceErrorException(AuthErrorEnum.INVALID_ADMIN_SECRET_KEY);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new ServiceErrorException(AuthErrorEnum.DUPLICATED_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.ofAdmin(request.email(), encodedPassword);
        userRepository.save(user);

        return new AuthSignupResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
