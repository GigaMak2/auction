package com.example.auction.common.initializer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import com.example.auction.domain.auction.search.document.AuctionDocument;
import com.example.auction.domain.auction.search.util.AuctionDocumentUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(
    name = "spring.elasticsearch.create-auction-index",
    havingValue = "true",
    matchIfMissing = false
)
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexInitializer implements ApplicationRunner {
    private final ElasticsearchOperations elasticsearch;
    
    public void run(ApplicationArguments args) throws Exception {
        final String alias = AuctionDocumentUtil.ALIAS_NAME;
        IndexOperations aliasOps = elasticsearch.indexOps(IndexCoordinates.of(alias));

        if (aliasOps.exists()) {
            // auction-auction alias가 있고, 달린 index가 있는지 확인
            Map<String, Set<AliasData>> bound = aliasOps.getAliases(alias);
            if (!bound.isEmpty()) {
                log.info("[ElasticsearchIndexInitializer] '{}' 인덱스가 가 이미 '{}' alias를 가지고 있음, init skip", 
                            bound.keySet(), alias);
                return;
            }
        }

        // alias로 풀리진 않지만, auction-auction 이라는 '실제 index'가 존재하는 경우 -> 설정 오류
        if (aliasOps.exists()) {
            throw new IllegalStateException(
                "%s 라는 이름을 가진 실제 index가 존재 합니다. %s는 alias여야 합니다".formatted(alias, alias));
        }

        // physical index를 생성
        String indexName = AuctionDocumentUtil.getNewIndexName(LocalDateTime.now());
        IndexOperations targetOps = elasticsearch.indexOps(IndexCoordinates.of(indexName));

        IndexOperations docOps = elasticsearch.indexOps(AuctionDocument.class);
        targetOps.create(
                docOps.createSettings(AuctionDocument.class),
                docOps.createMapping(AuctionDocument.class));
        log.info("[ElasticsearchIndexInitializer] 인덱스 생성 - indexName={}", indexName);

        // auction-auction alias를 생성
        targetOps.alias(new AliasActions(new AliasAction.Add(
                AliasActionParameters.builder()
                        .withIndices(indexName)
                        .withAliases(alias)
                        .build())));

        log.info("[ElasticsearchIndexInitializer] alias '{}' -> '{}' 생성", alias, indexName);
    }
}
