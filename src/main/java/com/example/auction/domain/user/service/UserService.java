package com.example.auction.domain.user.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.user.dto.UserGetResponse;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserGetResponse myPage(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        return new UserGetResponse(
                user.getId(),
                user.getEmail(),
                user.getRating(),
                user.getRole()
        );
    }
}
