package com.example.auction.domain.auction.repository;
import static com.example.auction.domain.auction.entity.QAuction.auction;

import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.category.service.CategoryService;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class CustomAuctionRepositoryImpl implements CustomAuctionRepository{

    private final JPAQueryFactory queryFactory;
    private final CategoryService categoryService;

    @Override
    public Page<@NonNull Auction> findByCondition(
            AuctionSearchCondition condition
    ) {
        return findByConditionImpl(null, condition);
    }

    @Override
    public Page<@NonNull Auction> findByUserIdAndCondition(
            Long userId,
            AuctionSearchCondition condition
    ) {
        return findByConditionImpl(userId, condition);
    }

    @Override
    public Page<AuctionAdminListResponse> findAuctionWithConditions(Pageable pageable, AuctionStatus auctionStatus, String keyword) {
        List<AuctionAdminListResponse> list = queryFactory
                .select(Projections.constructor(AuctionAdminListResponse.class,
                        auction.id,
                        auction.userId,
                        auction.itemName,
                        auction.categoryId,
                        auction.status,
                        auction.createdAt,
                        auction.startedAt,
                        auction.endedAt,
                        auction.cancelledAt))
                .from(auction)
                .where(
                        statusEq(auctionStatus),
                        keywordContains(keyword)
                )
                .orderBy(auction.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(auction.count())
                .from(auction)
                .where(
                        statusEq(auctionStatus),
                        keywordContains(keyword)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    private Page<@NonNull Auction> findByConditionImpl(
            @Nullable Long userId,
            AuctionSearchCondition condition
    ) {
        PageRequest pageRequest = PageRequest.of(
            condition.getPage(),
            condition.getPageSize()
        );

        BooleanExpression[] booleans = new BooleanExpression[] {
                statusContains(condition),
                inMaxPriceRange(condition),
                hasCategory(condition),
                isOwnedBy(userId),
                hasKeyword(condition)
        };

        List<Auction> auctions = queryFactory
            .selectFrom(auction)
            .where(booleans)
            .orderBy(new OrderSpecifier<>(Order.DESC, auction.createdAt))
            .offset(pageRequest.getOffset())
            .limit(condition.getPageSize())
            .fetch();

        JPAQuery<Long> totalCount = queryFactory
            .select(auction.count())
            .from(auction)
            .where(booleans);

        return PageableExecutionUtils.getPage(auctions, pageRequest, () -> totalCount.fetchOne());
    }

    private BooleanExpression statusContains(AuctionSearchCondition condition) {
        Set<AuctionStatus> statuses = condition.getStatus();

        if (statuses != null && !statuses.isEmpty()) {
            return auction.status.in(statuses);
        }

        return null;
    }

    private BooleanExpression inMaxPriceRange(AuctionSearchCondition condition) {
         if(condition.getMaxPriceMin() != null && condition.getMaxPriceMax() != null) {

            return auction.maxPrice.between(condition.getMaxPriceMin(), condition.getMaxPriceMax());

         } else if (condition.getMaxPriceMin() != null) {

            return auction.maxPrice.goe(condition.getMaxPriceMin());

         } else if (condition.getMaxPriceMax() != null) {

            return auction.maxPrice.loe(condition.getMaxPriceMax());

         } else {
            return null;
         }
    }

    private BooleanExpression hasCategory(AuctionSearchCondition condition) {
        if (condition.getCategoryId() == null) {
            return null;
        }

        List<Long> categoryIds = categoryService.collectDescendantIds(condition.getCategoryId());
        return auction.categoryId.in(categoryIds);
    }

    private BooleanExpression isOwnedBy(@Nullable Long userId) {
        if (userId != null) {
            return auction.userId.eq(userId);
        }

        return null;
    }

    private BooleanExpression hasKeyword(AuctionSearchCondition condition) {
        // TODO: 일단 간단한 like 키워드로만 검색
        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
            return auction.itemName.containsIgnoreCase(condition.getKeyword().trim());
        }

        return null;
    }

    private BooleanExpression statusEq(AuctionStatus status) {
        return status != null ? auction.status.eq(status) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword) ? auction.itemName.containsIgnoreCase(keyword) : null;
    }
}
