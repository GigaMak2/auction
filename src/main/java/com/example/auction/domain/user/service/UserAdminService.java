package com.example.auction.domain.user.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.user.dto.UserDetailGetResponse;
import com.example.auction.domain.user.dto.UserListGetResponse;
import com.example.auction.domain.user.dto.UserSearchCondition;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.entity.UserSocialAccount;
import com.example.auction.domain.user.enums.AuthProvider;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.user.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserListGetResponse> getUserList(UserSearchCondition condition) {
        Page<UserListGetResponse> userList = userRepository.findUsersWithConditions(
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getDeleted(),
                condition.getRole(),
                condition.getEmail()
        );

        return PageResponse.create(userList);
    }

    @Transactional(readOnly = true)
    public UserDetailGetResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        AuthProvider authProvider = userSocialAccountRepository.findByUserId(userId)
                .map(UserSocialAccount::getProvider)
                .orElse(null);

        return new UserDetailGetResponse(
                user.getId(),
                user.getEmail(),
                user.getRating(),
                user.getRole(),
                authProvider,
                user.isDeleted(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }
}
