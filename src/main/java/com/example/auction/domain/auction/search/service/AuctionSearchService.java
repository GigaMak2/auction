package com.example.auction.domain.auction.search.service;

import com.example.auction.domain.auction.repository.AuctionRepository;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.enums.AuctionStatus;
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
        try {
            return auctionElasticsearchService.searchAuctionFromElasticsearch(condition);
        } catch (Exception e) {
            log.error("[AuctionSearch] elasticsearch 검색 실패, DB 검색으로 fallback - {}",
                    condition.toLogString(), e);

            return searchAuctionFromDb(condition);
        }
    }

    public Page<AuctionSearchResult> searchAuction(
            Long userId,
            AuctionSearchCondition condition
    ) {
        try {
            return auctionElasticsearchService.searchAuctionFromElasticsearch(userId, condition);
        } catch (Exception e) {
            log.error("[AuctionSearch] elasticsearch 검색 실패, DB 검색으로 fallback - userId={}, {}",
                    userId, condition.toLogString(), e);

            return searchAuctionFromDb(userId, condition);
        }
    }

    public Page<AuctionAdminListResponse> searchAuctionWithConditions(
            Pageable pageable, AuctionStatus auctionStatus, String keyword
    ) {
        try {
            return auctionElasticsearchService.searchAuctionWithConditionsFromElasticsearch(pageable, auctionStatus, keyword);
        } catch (Exception e) {
            log.error("[AuctionSearch] elasticsearch 검색 실패, DB 검색으로 fallback - pageable={}, auctionStatus={}, keyword={}",
                    pageable, auctionStatus, keyword, e);
            return auctionRepository.findAuctionWithConditions(pageable, auctionStatus, keyword);
        }
    }
}
