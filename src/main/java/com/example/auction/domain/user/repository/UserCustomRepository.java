package com.example.auction.domain.user.repository;

import com.example.auction.domain.user.dto.UserListGetResponse;
import com.example.auction.domain.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserCustomRepository {
    Page<UserListGetResponse> findUsersWithConditions(Pageable pageable, Boolean deleted, UserRole role, String email);
}
