package com.example.auction.domain.user.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.user.dto.UserListGetResponse;
import com.example.auction.domain.user.dto.UserSearchCondition;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;

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
}
