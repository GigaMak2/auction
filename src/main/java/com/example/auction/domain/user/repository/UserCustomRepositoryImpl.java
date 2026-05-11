package com.example.auction.domain.user.repository;

import com.example.auction.domain.user.dto.response.UserListGetResponse;
import com.example.auction.domain.user.enums.UserRole;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.auction.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<UserListGetResponse> findUsersWithConditions(Pageable pageable, Boolean deleted, UserRole role, String email) {
        List<UserListGetResponse> list = queryFactory
                .select(Projections.constructor(UserListGetResponse.class,
                        user.id,
                        user.email,
                        user.role,
                        user.deleted,
                        user.createdAt,
                        user.deletedAt))
                .from(user)
                .where(
                        deletedEq(deleted),
                        roleEq(role),
                        emailContains(email)
                )
                .orderBy(user.deleted.asc(), user.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        deletedEq(deleted),
                        roleEq(role),
                        emailContains(email)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    private BooleanExpression deletedEq(Boolean deleted) {
        return deleted != null ? user.deleted.eq(deleted) : null;
    }

    private BooleanExpression roleEq(UserRole role) {
        return role != null ? user.role.eq(role) : null;
    }

    private BooleanExpression emailContains(String email) {
        return StringUtils.hasText(email) ? user.email.containsIgnoreCase(email) : null;
    }
}
