package com.example.auction.domain.userchat.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserChatMessageCustomRepositoryImpl implements UserChatMessageCustomRepository {

    private final JPAQueryFactory queryFactory;
}
