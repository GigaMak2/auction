package com.example.auction.domain.bid.repository;

import com.example.auction.domain.bid.entity.Bid;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
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
                        user.deleted.isFalse()
                )
                .orderBy(bid.price.asc(), bid.createdAt.asc())
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
