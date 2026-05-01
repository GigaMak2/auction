package com.example.auction.domain.auction.search.service;

import java.util.ArrayList;
import java.util.List;

import com.example.auction.domain.auction.repository.AuctionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.search.document.AuctionDocument;
import com.example.auction.domain.auction.search.dto.AuctionCreatedDocument;
import com.example.auction.domain.auction.search.dto.AuctionSearchResult;
import com.example.auction.domain.auction.util.AuctionUtil;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionSearchService {
    private final ElasticsearchOperations elasticsearch;

    private final PlatformTransactionManager txManager;
    private final AuctionRepository auctionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionCreated(AuctionCreatedDocument event) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                AuctionDocument doc = AuctionDocument.from(event);
                elasticsearch.save(doc);
                return; // 성공하면 종료
            } catch (Exception e) {
                log.warn("[AuctionSearch] AuctionDocument 등록 실패 {}/{}회 - auctionId={}",
                        attempt, maxAttempts, event.id(), e);
                if (attempt == maxAttempts) {
                    log.error("[AuctionSearch] AuctionDocument 등록 최종 실패 - auctionId={}",
                            event.id(), e);
                }

                // TODO: exponential backoff 적용
            }
        }
    }

    public Page<AuctionSearchResult> searchAuctionFromElasticsearch (
            AuctionSearchCondition condition
    ) {
        AuctionUtil.throwIfSearchConditionNotValid(condition);

        List<Query> mustQueries = new ArrayList<>();

        if (condition.getMaxPriceMin() != null || condition.getMaxPriceMax() != null) {
            NumberRangeQuery.Builder nrqBuilder = new NumberRangeQuery.Builder().field("maxPrice");

            if (condition.getMaxPriceMin() != null) {
                nrqBuilder.gte(condition.getMaxPriceMin().doubleValue());
            }

            if (condition.getMaxPriceMax() != null) {
                nrqBuilder.lte(condition.getMaxPriceMax().doubleValue());
            }

            Query priceRange = QueryBuilders.range().number(nrqBuilder.build()).build()._toQuery();

            mustQueries.add(priceRange);
        }

        if (condition.getStatus() != null) {
            List<FieldValue> statusFieldValues = condition.getStatus().stream()
                .map(s -> FieldValue.of(s.name()))
                .toList();

            Query statusQuery = QueryBuilders.terms()
                .field("status")
                .terms(t -> t.value(statusFieldValues))
                .build()
                ._toQuery();

            mustQueries.add(statusQuery);
        }

        if (condition.getCategoryId() != null) {
            Query categoryQuery = QueryBuilders.term()
                .field("categoryId")
                .value(FieldValue.of(condition.getCategoryId()))
                .build()
                ._toQuery();

            mustQueries.add(categoryQuery);
        }

        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
            Query nameQuery = QueryBuilders.match()
                .query(condition.getKeyword())
                .field("itemName")
                .build()
                ._toQuery();

            mustQueries.add(nameQuery);
        }

        Query finalQuery;

        if (mustQueries.isEmpty()) {
            finalQuery = QueryBuilders.matchAll().build()._toQuery();
        } else {
            finalQuery = QueryBuilders.bool().must(mustQueries).build()._toQuery();
        }

        PageRequest pageRequest = PageRequest.of(
                condition.getPage(),
                condition.getPageSize()
                );

        SearchHits<AuctionDocument> results = elasticsearch.search(
                NativeQuery.builder()
                .withQuery(finalQuery)
                .withPageable(pageRequest)
                .build(),
                AuctionDocument.class);

        return SearchHitSupport.searchPageFor(results, pageRequest).map(x -> AuctionSearchResult.from(x.getContent()));
    }

    public Page<AuctionSearchResult> searchAuctionFromDb (
            AuctionSearchCondition condition
    ) {
        AuctionUtil.throwIfSearchConditionNotValid(condition);

        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.setReadOnly(true);

        return tx.execute(status-> auctionRepository.findByCondition(condition).map(AuctionSearchResult::from));
    }

    public Page<AuctionSearchResult> searchAuction (
            AuctionSearchCondition condition
    ) {
        AuctionUtil.throwIfSearchConditionNotValid(condition);

        if (condition.getKeyword() == null || condition.getKeyword().isBlank()) {
            try {
                return searchAuctionFromDb(condition);
            } catch (Exception e) {
                log.error("[AuctionSearch] DB 키워드 없는 검색 실패, elasticsearch로 fallback - {}",
                        condition.toLogString(), e);

                return searchAuctionFromElasticsearch(condition);
            }
        }

        return searchAuctionFromElasticsearch(condition);
    }
}
