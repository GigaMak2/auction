package com.example.auction.domain.ai.tool;

import com.example.auction.domain.ai.tool.dto.AuctionBidInfo;
import com.example.auction.domain.ai.tool.dto.AuctionResultInfo;
import com.example.auction.domain.ai.tool.dto.CategoryAuctionStats;
import com.example.auction.domain.ai.tool.dto.MyAuctionInfo;
import com.example.auction.domain.ai.tool.dto.MyBidInfo;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.category.entity.Category;
import com.example.auction.domain.category.repository.CategoryRepository;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.auction.domain.bid.entity.QBid;

import static com.example.auction.domain.auction.entity.QAuction.auction;
import static com.example.auction.domain.auction.result.entity.QAuctionResult.auctionResult;
import static com.example.auction.domain.bid.entity.QBid.bid;
import static com.example.auction.domain.category.entity.QCategory.category;
import static com.example.auction.domain.review.entity.QReview.review;

@Repository
@RequiredArgsConstructor
public class AiToolRepositoryImpl implements AiToolRepository {

    private static final int MAX_BIDS_FOR_TOOL = 50;

    private final JPAQueryFactory queryFactory;
    private final CategoryRepository categoryRepository;

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

    @Override
    public List<MyAuctionInfo> findMyAuctions(Long userId) {
        QBid bidSub = new QBid("bidSub");
        return queryFactory
                .select(auction.id, auction.itemName, auction.status, auction.endedAt,
                        JPAExpressions.select(bidSub.price.min())
                                .from(bidSub)
                                .where(bidSub.auctionId.eq(auction.id)))
                .from(auction)
                .where(auction.userId.eq(userId))
                .orderBy(
                        new CaseBuilder()
                                .when(auction.status.in(AuctionStatus.ACTIVE, AuctionStatus.READY)).then(0)
                                .otherwise(1).asc(),
                        auction.endedAt.asc()
                )
                .limit(10)
                .fetch()
                .stream()
                .map(t -> new MyAuctionInfo(
                        t.get(auction.id),
                        t.get(auction.itemName),
                        t.get(auction.status).name(),
                        t.get(auction.endedAt),
                        t.get(4, BigDecimal.class)
                ))
                .toList();
    }

    @Override
    public List<CategoryAuctionStats> findAuctionStatsByCategory(String categoryName) {
        Set<Long> categoryIds = resolveDescendantCategoryIds(categoryName);
        if (categoryIds.isEmpty()) return List.of();

        return queryFactory
                .select(category.name,
                        auctionResult.price.avg(),
                        auctionResult.price.min(),
                        auctionResult.price.max(),
                        auctionResult.count())
                .from(auctionResult)
                .join(auction).on(auctionResult.auctionId.eq(auction.id))
                .join(category).on(category.id.eq(auction.categoryId))
                .where(category.id.in(categoryIds))
                .groupBy(category.name)
                .fetch()
                .stream()
                .map(t -> new CategoryAuctionStats(
                        t.get(category.name),
                        t.get(auctionResult.price.avg()),
                        t.get(auctionResult.price.min()),
                        t.get(auctionResult.price.max()),
                        t.get(auctionResult.count())
                ))
                .toList();
    }

    // name 일치 카테고리 + 자식/손자(depth 최대 2) ID를 전부 수집
    private Set<Long> resolveDescendantCategoryIds(String categoryName) {
        List<Category> matched = categoryRepository.findByNameContainingIgnoreCase(categoryName);
        Set<Long> ids = new HashSet<>();
        for (Category cat : matched) {
            ids.add(cat.getId());
            for (Category child : categoryRepository.findAllByParentId(cat.getId())) {
                ids.add(child.getId());
                categoryRepository.findAllByParentId(child.getId())
                        .forEach(gc -> ids.add(gc.getId()));
            }
        }
        return ids;
    }

    @Override
    public List<MyBidInfo> findMyBids(Long userId) {
        QBid bidSub = new QBid("bidSub");
        return queryFactory
                .select(auction.id, auction.itemName, auction.status, auction.endedAt,
                        bid.price.min(),
                        JPAExpressions.select(bidSub.price.min())
                                .from(bidSub)
                                .where(bidSub.auctionId.eq(auction.id)))
                .from(bid)
                .join(auction).on(auction.id.eq(bid.auctionId))
                .where(bid.userId.eq(userId))
                .groupBy(auction.id, auction.itemName, auction.status, auction.endedAt)
                .orderBy(
                        new CaseBuilder()
                                .when(auction.status.eq(AuctionStatus.ACTIVE)).then(0)
                                .otherwise(1).asc(),
                        auction.endedAt.desc()
                )
                .limit(10)
                .fetch()
                .stream()
                .map(t -> {
                    BigDecimal myLowest = t.get(bid.price.min());
                    BigDecimal currentLowest = t.get(5, BigDecimal.class);
                    return new MyBidInfo(
                            t.get(auction.id),
                            t.get(auction.itemName),
                            t.get(auction.status).name(),
                            t.get(auction.endedAt),
                            myLowest,
                            currentLowest,
                            myLowest != null && currentLowest != null && myLowest.compareTo(currentLowest) == 0
                    );
                })
                .toList();
    }
}