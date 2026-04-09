package com.example.auction.domain.user.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.user.dto.UserSignupRequest;
import com.example.auction.domain.user.dto.UserSignupResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
