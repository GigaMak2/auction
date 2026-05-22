package com.example.auction.domain.auction.search.entity;

import com.example.auction.common.entity.CreatableEntity;
import com.example.auction.domain.auction.search.enums.AuctionDocumentReindexJobStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "auction_document_reindex_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionDocumentReindexJob extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String newIndexName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionDocumentReindexJobStatus jobStatus;

    @Column(nullable = false)
    private Long auctionIdCursor;

    public static AuctionDocumentReindexJob of(
            String newIndexName
    ) {
        AuctionDocumentReindexJob job = new AuctionDocumentReindexJob();

        job.newIndexName = newIndexName;
        job.jobStatus = AuctionDocumentReindexJobStatus.IN_PROGRESS;
        job.auctionIdCursor = 0L;

        return job;
    }

    public void updateAuctionIdCursor(Long newCursorPos) {
        this.auctionIdCursor = newCursorPos;
    }

    public void updateJobStatus(AuctionDocumentReindexJobStatus newStatus) {
        this.jobStatus = newStatus;
    }
}
