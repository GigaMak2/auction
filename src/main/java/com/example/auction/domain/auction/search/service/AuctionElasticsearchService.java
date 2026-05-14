package com.example.auction.domain.auction.search.service;

import java.util.ArrayList;
import java.util.List;

import co.elastic.clients.elasticsearch._types.*;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.auction.domain.auction.dto.response.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.request.AuctionSearchCondition;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.search.document.AuctionDocument;
import com.example.auction.domain.auction.search.dto.AuctionCreatedDocument;
import com.example.auction.domain.auction.search.dto.AuctionSearchResult;
import com.example.auction.domain.auction.util.AuctionUtil;

import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionElasticsearchService {
    private final ElasticsearchOperations elasticsearch;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionCreated(AuctionCreatedDocument event) {
        int maxAttempts = 3;

        int backoffBaseMilli = 500;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                AuctionDocument doc = AuctionDocument.from(event);
                elasticsearch.save(doc);
                return;
            } catch (Exception e) {
                log.warn("[AuctionSearch] AuctionDocument 등록 실패 {}/{}회 - auctionId={}",
                        attempt, maxAttempts, event.id(), e);
                if (attempt == maxAttempts) {
                    log.error("[AuctionSearch] AuctionDocument 등록 최종 실패 - auctionId={}",
                            event.id(), e);
                }else {
                    try {
                        Thread.sleep(backoffBaseMilli * (1L << attempt));
                    } catch(InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private Page<AuctionSearchResult> searchAuctionFromElasticsearchImpl (
            @Nullable Long userId,
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

        if (userId != null) {
            Query userIdQuery = QueryBuilders.term()
                .field("userId")
                .value(userId)
                .build()
                ._toQuery();

            mustQueries.add(userIdQuery);
        }

        Query finalQuery;

        if (mustQueries.isEmpty()) {
            finalQuery = QueryBuilders.matchAll().build()._toQuery();
        } else {
            finalQuery = QueryBuilders.bool().must(mustQueries).build()._toQuery();
        }

        List<SortOptions> sortOptions = new ArrayList<>();
        sortOptions.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
        sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc))));

        PageRequest pageRequest = PageRequest.of(
                condition.getPage(),
                condition.getSize()
                );

        SearchHits<AuctionDocument> results = elasticsearch.search(
                NativeQuery.builder()
                .withQuery(finalQuery)
                .withSort(sortOptions)
                .withPageable(pageRequest)
                .build(),
                AuctionDocument.class);

        return SearchHitSupport.searchPageFor(results, pageRequest).map(x -> AuctionSearchResult.from(x.getContent()));
    }

    public Page<AuctionSearchResult> searchAuctionFromElasticsearch(
            Long userId,
            AuctionSearchCondition condition
    ) {
        return searchAuctionFromElasticsearchImpl(userId, condition);
    }

    public Page<AuctionSearchResult> searchAuctionFromElasticsearch(
            AuctionSearchCondition condition
    ) {
        return searchAuctionFromElasticsearchImpl(null, condition);
    }

    public Page<AuctionAdminListResponse> searchAuctionWithConditionsFromElasticsearch (
            Pageable pageable, AuctionStatus auctionStatus, String keyword
    ) {
        List<Query> mustQueries = new ArrayList<>();

        if (auctionStatus != null) {
            Query statusQuery = QueryBuilders.term()
                .field("status")
                .value(FieldValue.of(auctionStatus.name()))
                .build()
                ._toQuery();

            mustQueries.add(statusQuery);
        }

        if (keyword != null && !keyword.isBlank()) {
            Query nameQuery = QueryBuilders.match()
                .query(keyword)
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

        List<SortOptions> sortOptions = new ArrayList<>();
        sortOptions.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
        sortOptions.add(SortOptions.of(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc))));

        SearchHits<AuctionDocument> results = elasticsearch.search(
                NativeQuery.builder()
                .withQuery(finalQuery)
                .withSort(sortOptions)
                .withPageable(pageable)
                .build(),
                AuctionDocument.class);

        return SearchHitSupport.searchPageFor(results, pageable)
            .map(x -> {
                AuctionDocument doc = x.getContent();
                return new AuctionAdminListResponse(
                        doc.getId(),
                        doc.getUserId(),
                        doc.getItemName(),
                        doc.getCategoryId(),
                        doc.getStatus(),
                        doc.getCreatedAt(),
                        doc.getStartedAt(),
                        doc.getEndedAt(),
                        doc.getCancelledAt()
                );
            });
    }
}
