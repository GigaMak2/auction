package com.example.auction.domain.ai.tool;

import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.auction.domain.auction.entity.QAuction.auction;
import static com.example.auction.domain.auction.result.entity.QAuctionResult.auctionResult;
import static com.example.auction.domain.bid.entity.QBid.bid;
import static com.example.auction.domain.review.entity.QReview.review;

@Repository
@RequiredArgsConstructor
public class AiToolRepositoryImpl implements AiToolRepository {

    private static final int MAX_BIDS_FOR_TOOL = 50;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AuctionBidInfo> findBidsByAuctionId(Long auctionId) {
        return queryFactory
                .select(bid.id, bid.userId, bid.price, bid.createdAt)
                .from(bid)
                .where(bid.auctionId.eq(auctionId))
                .orderBy(bid.price.asc(), bid.createdAt.asc(), bid.id.asc())
                .limit(MAX_BIDS_FOR_TOOL)
                .fetch()
                .stream()
                .map(t -> new AuctionBidInfo(
                        t.get(bid.id),
                        t.get(bid.userId),
                        t.get(bid.price),
                        t.get(bid.createdAt)
                ))
                .toList();
    }

    @Override
    public List<AuctionResultInfo> findRecentAuctionResultsByItemName(String itemName) {
        return queryFactory
                .select(auction.itemName, auctionResult.price, auction.endedAt)
                .from(auctionResult)
                .join(auction).on(auctionResult.auctionId.eq(auction.id))
                .where(auction.itemName.containsIgnoreCase(itemName))
                .orderBy(auction.endedAt.desc())
                .limit(10)
                .fetch()
                .stream()
                .map(t -> new AuctionResultInfo(
                        t.get(auction.itemName),
                        t.get(auctionResult.price),
                        t.get(auction.endedAt)
                ))
                .toList();
    }

    @Override
    public long countSellerSales(Long sellerId) {
        Long count = queryFactory
                .select(auctionResult.count())
                .from(auctionResult)
                .where(auctionResult.sellerId.eq(sellerId))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public List<String> findRecentReviewTextsBySellerId(Long sellerId) {
        return queryFactory
                .select(review.description)
                .from(review)
                .where(
                        review.revieweeId.eq(sellerId),
                        review.description.isNotNull(),
                        Expressions.stringTemplate("trim({0})", review.description).ne("")
                )
                .orderBy(review.createdAt.desc())
                .limit(5)
                .fetch();
    }
}