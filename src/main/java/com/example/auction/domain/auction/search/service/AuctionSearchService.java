package com.example.auction.domain.auction.search.service;

import com.example.auction.domain.auction.repository.AuctionRepository;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.search.dto.AuctionSearchResult;
import com.example.auction.domain.auction.util.AuctionUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class AuctionSearchService {
    private final TransactionTemplate txTemplate;
    private final AuctionRepository auctionRepository;

    private final AuctionElasticsearchService auctionElasticsearchService;

    public AuctionSearchService(
            PlatformTransactionManager txManager,
            AuctionRepository auctionRepository,
            AuctionElasticsearchService auctionElasticsearchService
    ) {
        this.txTemplate = new TransactionTemplate(txManager);
        txTemplate.setReadOnly(true);

        this.auctionRepository = auctionRepository;
        this.auctionElasticsearchService = auctionElasticsearchService;
    }

    private Page<AuctionSearchResult> searchAuctionFromDbImpl(
            @Nullable Long userId,
            AuctionSearchCondition condition
    ) {
        AuctionUtil.throwIfSearchConditionNotValid(condition);

        if (userId == null) {
            return txTemplate.execute(status -> auctionRepository.findByCondition(condition).map(AuctionSearchResult::from));
        } else {
            return txTemplate.execute(status -> auctionRepository.findByUserIdAndCondition(userId, condition).map(AuctionSearchResult::from));
        }
    }

    public Page<AuctionSearchResult> searchAuctionFromDb(
            Long userId,
            AuctionSearchCondition condition
    ) {
        return searchAuctionFromDbImpl(userId, condition);
    }

    public Page<AuctionSearchResult> searchAuctionFromDb(
            AuctionSearchCondition condition
    ) {
        return searchAuctionFromDbImpl(null, condition);
    }

    public Page<AuctionSearchResult> searchAuction(
            AuctionSearchCondition condition
    ) {
        if (condition.getKeyword() == null || condition.getKeyword().isBlank()) {
            try {
                return searchAuctionFromDb(condition);
            } catch (Exception e) {
                log.error("[AuctionSearch] DB 키워드 없는 검색 실패, elasticsearch로 fallback - {}",
                        condition.toLogString(), e);

                return auctionElasticsearchService.searchAuctionFromElasticsearch(condition);
            }
        }

        return auctionElasticsearchService.searchAuctionFromElasticsearch(condition);
    }

    public Page<AuctionSearchResult> searchAuction(
            Long userId,
            AuctionSearchCondition condition
    ) {
        if (condition.getKeyword() == null || condition.getKeyword().isBlank()) {
            try {
                return searchAuctionFromDb(userId, condition);
            } catch (Exception e) {
                log.error("[AuctionSearch] DB 키워드 없는 검색 실패, elasticsearch로 fallback - userId={}, {}",
                        userId, condition.toLogString(), e);

                return auctionElasticsearchService.searchAuctionFromElasticsearch(userId, condition);
            }
        }

        return auctionElasticsearchService.searchAuctionFromElasticsearch(userId, condition);
    }
}
