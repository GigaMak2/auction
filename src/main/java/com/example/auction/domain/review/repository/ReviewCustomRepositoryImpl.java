package com.example.auction.domain.review.repository;

import com.example.auction.domain.review.dto.ReviewAdminListResponse;
import com.example.auction.domain.review.dto.ReviewListGetResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.example.auction.domain.review.entity.QReview.review;

@RequiredArgsConstructor
public class ReviewCustomRepositoryImpl implements ReviewCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ReviewListGetResponse> findWrittenReviewsWithConditions(Long userId, Pageable pageable, LocalDate startDate, LocalDate endDate) {
        BooleanExpression dateCondition = dateBetween(startDate, endDate);

        List<ReviewListGetResponse> list = queryFactory
                .select(Projections.constructor(ReviewListGetResponse.class,
                        review.id,
                        review.auctionId,
                        review.revieweeId,
                        review.createdAt,
                        review.modifiedAt,
                        review.imageUrl))
                .from(review)
                .where(
                        review.reviewerId.eq(userId),
                        dateCondition
                )
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(review.count())
                .from(review)
                .where(
                        review.reviewerId.eq(userId),
                        dateCondition
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    @Override
    public Page<ReviewListGetResponse> findReceivedReviewsWithConditions(Long userId, Pageable pageable, LocalDate startDate, LocalDate endDate) {
        BooleanExpression dateCondition = dateBetween(startDate, endDate);

        List<ReviewListGetResponse> list = queryFactory
                .select(Projections.constructor(ReviewListGetResponse.class,
                        review.id,
                        review.auctionId,
                        review.reviewerId,
                        review.createdAt,
                        review.modifiedAt,
                        review.imageUrl))
                .from(review)
                .where(
                        review.revieweeId.eq(userId),
                        dateCondition
                )
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(review.count())
                .from(review)
                .where(
                        review.revieweeId.eq(userId),
                        dateCondition
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    @Override
    public Page<ReviewAdminListResponse> findReviewsWithConditions(Pageable pageable, Long auctionId, Long reviewerId, Long revieweeId, Integer score) {
        List<ReviewAdminListResponse> list = queryFactory
                .select(Projections.constructor(ReviewAdminListResponse.class,
                        review.id,
                        review.auctionId,
                        review.reviewerId,
                        review.revieweeId,
                        review.score,
                        review.createdAt))
                .from(review)
                .where(
                        auctionIdEq(auctionId),
                        reviewerIdEq(reviewerId),
                        revieweeIdEq(revieweeId),
                        scoreEq(score)
                )
                .orderBy(review.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(review.count())
                .from(review)
                .where(
                        auctionIdEq(auctionId),
                        reviewerIdEq(reviewerId),
                        revieweeIdEq(revieweeId),
                        scoreEq(score)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    private BooleanExpression dateBetween(LocalDate startDate, LocalDate endDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : now.toLocalDate().minusMonths(6).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : now;
        return review.createdAt.between(start, end);
    }

    private BooleanExpression auctionIdEq(Long auctionId) {
        return auctionId != null ? review.auctionId.eq(auctionId) : null;
    }

    private BooleanExpression reviewerIdEq(Long reviewerId) {
        return reviewerId != null ? review.reviewerId.eq(reviewerId) : null;
    }

    private BooleanExpression revieweeIdEq(Long revieweeId) {
        return revieweeId != null ? review.revieweeId.eq(revieweeId) : null;
    }

    private BooleanExpression scoreEq(Integer score) {
        return score != null ? review.score.eq(score): null;
    }
}
