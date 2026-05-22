package com.example.auction.domain.auction.search.service;

import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.search.dto.AuctionCreatedDocument;
import com.example.auction.domain.auction.search.entity.AuctionDocumentReindexJob;
import com.example.auction.domain.auction.search.repository.AuctionDocumentReindexJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionDocumentReindexServiceHelper {

    private final AuctionRepository auctionRepository;
    private final AuctionDocumentReindexJobRepository jobRepository;

     private final ElasticsearchOperations operations;

    @Transactional
    public void deleteOldReindexJobs(LocalDateTime before) {
        List<AuctionDocumentReindexJob> jobs = jobRepository.findAll();

        List<AuctionDocumentReindexJob> oldJobs = jobs.stream()
            .filter(j -> j.getCreatedAt().isBefore(before))
            .toList();

        jobRepository.deleteAll(oldJobs);
    }

    @Transactional
    public AuctionDocumentReindexJob saveJob(AuctionDocumentReindexJob job) {
        return jobRepository.saveAndFlush(job);
    }

    @Transactional(readOnly = true)
    public List<AuctionCreatedDocument> findAuctionsByCursor(Long auctionIdCursor, int size) {
        List<Auction> auctions = auctionRepository.findByCursor(auctionIdCursor, size);

        return auctions.stream()
                       .map(AuctionCreatedDocument::from)
                       .toList();
    }

    public List<String> pointAliasAtNewIndex(String aliasName, String newIndex) {
        // 원래 이 alias를 먼저 가지고 있던 index들을 구해오기
        Map<String, Set<AliasData>> aliasMap = new HashMap<>();
            

        AliasActions actions = new AliasActions();

        boolean aliasExists = operations.indexOps(IndexCoordinates.of(aliasName)).exists();

        // 기존에 alias를 가지고 있던 index들을 alias로 부터 때놓기
        if (aliasExists) {
            aliasMap = operations.indexOps(IndexCoordinates.of(aliasName)).getAliases(aliasName);

            actions.add(new AliasAction.Remove(
                    AliasActionParameters.builder()
                            .withIndices("*")
                            .withAliases(aliasName)
                            .build()));
        }

        // newIndex에 새 alias를 추가
        actions.add(new AliasAction.Add(
                AliasActionParameters.builder()
                        .withIndices(newIndex)
                        .withAliases(aliasName)
                        .build()));

        operations.indexOps(IndexCoordinates.of(newIndex)).alias(actions);

        return aliasMap.keySet().stream().toList();
    }
}
