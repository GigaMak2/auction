package com.example.auction.domain.auction.search.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.search.document.AuctionDocument;
import com.example.auction.domain.auction.search.dto.AuctionCreatedDocument;
import com.example.auction.domain.auction.search.entity.AuctionDocumentReindexJob;
import com.example.auction.domain.auction.search.enums.AuctionDocumentReindexJobStatus;
import com.example.auction.domain.auction.search.exception.AuctionSearchErrorEnum;
import com.example.auction.domain.auction.search.util.AuctionDocumentUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionDocumentReindexService {
    private final AuctionDocumentReindexServiceHelper helper;
    private final ElasticsearchOperations elasticsearch;

    @Scheduled(cron = "${scheduler.auction-reindex-job.cron}")
    @SchedulerLock(name = "auctionDocumentReindexService", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void reindexAuctionDocuments() {
        log.info("[AuctionDocumentReindexService] elasticsearch document reindexing 작업 시작");

        LocalDateTime beganTime = LocalDateTime.now();
        LocalDateTime monthAgo = beganTime.minusDays(30);

        // 오래된 작업 내역 들을 삭제
        helper.deleteOldReindexJobs(monthAgo);

        String newIndexName = AuctionDocumentUtil.getNewIndexName(beganTime);

        AuctionDocumentReindexJob job = helper.saveJob(AuctionDocumentReindexJob.of(newIndexName));

        try {
            // 백업을 할 새 index 생성
            IndexCoordinates target = IndexCoordinates.of(newIndexName);
            var indexOps = elasticsearch.indexOps(AuctionDocument.class);
            elasticsearch.indexOps(target).create(
                    indexOps.createSettings(AuctionDocument.class),
                    indexOps.createMapping(AuctionDocument.class)
            );
            log.info("[AuctionDocumentReindexService] 새 elasticsearch index 생성 - newIndexName={}", newIndexName);

            while(true) {
                List<AuctionCreatedDocument> auctions = helper.findAuctionsByCursor(job.getAuctionIdCursor(), 1000);
                if (auctions.size() <= 0) {
                    break;
                }

                log.info(
                    "[AuctionDocumentReindexService] 경매 id {} - {} reindexing 작업 시작",
                    auctions.getFirst().id(), auctions.getLast().id()
                );

                long minutesPassed = ChronoUnit.MINUTES.between(beganTime, LocalDateTime.now());
                if (minutesPassed > 60) {
                    log.error("[AuctionDocumentReindexService] 시간이 모자라 reindexing 작업 실패");

                    throw new ServiceErrorException(AuctionSearchErrorEnum.AUCTION_REINDEXING_OUT_OF_TIME);
                }

                // Auction을 Elasticsearch에 넣는 query 생성
                List<IndexQuery> queries = new ArrayList<>();

                Long newCursorPos = job.getAuctionIdCursor();

                for (AuctionCreatedDocument auction : auctions) {
                    AuctionDocument doc = AuctionDocument.from(auction);

                    IndexQuery query = IndexQuery.builder()
                        .withId(doc.getId().toString())
                        .withObject(doc)
                        .withOpType(IndexQuery.OpType.INDEX)
                        .build();

                    queries.add(query);

                    newCursorPos = auction.id();
                }

                // 실제 query 진행
                elasticsearch.bulkIndex(queries, IndexCoordinates.of(job.getNewIndexName()));

                job.updateAuctionIdCursor(newCursorPos);
                helper.saveJob(job);
            }

            List<String> oldInexes = helper.pointAliasAtNewIndex(AuctionDocumentUtil.ALIAS_NAME, newIndexName);
            elasticsearch.indexOps(IndexCoordinates.of(oldInexes.toArray(new String[0]))).delete();

            job.updateJobStatus(AuctionDocumentReindexJobStatus.DONE);
            helper.saveJob(job);
            log.info("[AuctionDocumentReindexService] elasticsearch document reindexing 작업 성공");
        } catch (Exception e) {
            log.error("[AuctionDocumentReindexService] elasticsearch document reindexing 작업 실패", e);

            job.updateJobStatus(AuctionDocumentReindexJobStatus.FAILED);
            helper.saveJob(job);

            throw e;
        }
    }
}
