package com.example.auction.domain.review.repository;

import com.example.auction.domain.review.dto.ReviewListByAuctionGetResponse;
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
import java.util.List;

import static com.example.auction.domain.review.entity.QReview.review;

@RequiredArgsConstructor
public class ReviewCustomRepositoryImpl implements ReviewCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ReviewListGetResponse> findReviewsWithConditions(Long userId, Pageable pageable, LocalDate startDate, LocalDate endDate) {
        List<ReviewListGetResponse> list = queryFactory
                .select(Projections.constructor(ReviewListGetResponse.class,
                        review.id,
                        review.auctionId,
                        review.revieweeId,
                        review.createdAt,
                        review.modifiedAt))
                .from(review)
                .where(
                        review.reviewerId.eq(userId),
                        dateBetween(startDate, endDate)
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
                        dateBetween(startDate, endDate)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    @Override
    public List<ReviewListByAuctionGetResponse> findByAuctionId(Long auctionId) {
        // TODO 경매 결과 연동되면 구매자 리뷰 상위에, 판매자 리뷰 하위에 노출
        return queryFactory
                .select(Projections.constructor(ReviewListByAuctionGetResponse.class,
                        review.id,
                        review.reviewerId,
                        review.revieweeId,
                        review.score,
                        review.description,
                        review.createdAt,
                        review.modifiedAt))
                .from(review)
                .where(review.auctionId.eq(auctionId))
                .orderBy(review.createdAt.desc())
                .fetch();
    }

    private BooleanExpression dateBetween(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atTime(0, 0, 0) : LocalDateTime.now().minusMonths(6);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();
        return review.createdAt.between(start, end);
    }
}
