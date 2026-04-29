package com.example.auction.domain.bid.repository;

import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static com.example.auction.domain.bid.entity.QBid.bid;
import static com.example.auction.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class BidCustomRepositoryImpl implements BidCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Bid> findFirstByAuctionIdOrderByPriceAsc(Long auctionId) {
        Bid result = queryFactory
                .selectFrom(bid)
                .join(user).on(bid.userId.eq(user.id))
                .where(
                        bid.auctionId.eq(auctionId),
                        user.deleted.isFalse(),
                        bid.status.eq(BidAuctionStatus.ACTIVE)
                )
                .orderBy(bid.price.asc(), bid.createdAt.asc())
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<BidAdminListResponse> findBidWithConditions(Pageable pageable, BidAuctionStatus status, Long auctionId, Long userId) {
        List<BidAdminListResponse> list = queryFactory
                .select(Projections.constructor(BidAdminListResponse.class,
                        bid.id,
                        bid.auctionId,
                        bid.userId,
                        bid.price,
                        bid.status,
                        bid.createdAt))
                .from(bid)
                .where(
                        statusEq(status),
                        auctionIdEq(auctionId),
                        userIdEq(userId)
                )
                .orderBy(bid.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(bid.count())
                .from(bid)
                .where(
                        statusEq(status),
                        auctionIdEq(auctionId),
                        userIdEq(userId)
                )
                .fetchOne();

        if (total == null) total = 0L;

        return new PageImpl<>(list, pageable, total);
    }

    private BooleanExpression statusEq(BidAuctionStatus status) {
        return status != null ? bid.status.eq(status) : null;
    }

    private BooleanExpression auctionIdEq(Long auctionId) {
        return auctionId != null ? bid.auctionId.eq(auctionId) : null;
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId != null ? bid.userId.eq(userId) : null;
    }
}
