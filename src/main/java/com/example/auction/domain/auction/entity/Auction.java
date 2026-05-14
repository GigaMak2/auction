package com.example.auction.domain.auction.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.auction.common.entity.CreatableEntity;
import com.example.auction.domain.auction.enums.AuctionStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Getter
@Entity
@Table(name = "auctions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auction extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Size(max=1024)
    @Column(name="description")
    private String description;

    @Column(name="max_price", nullable = false)
    private BigDecimal maxPrice;

    @Size(max=256)
    @Column(name="item_name", nullable = false)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name="auction_status", nullable = false)
    private AuctionStatus status;

    @Column(name="started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name="ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name="cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name="category_id", nullable = false)
    private Long categoryId;

    // tsvector는 Hibernate로 관리 불가 - JDBC로 직접 업데이트
    // 아래 필드는 QueryDSL path 전용
    @Column(columnDefinition = "tsvector", insertable = false, updatable = false)
    private String itemNameSearchVector;

    @Column(columnDefinition = "tsvector", insertable = false, updatable = false)
    private String descriptionSearchVector;

    private Integer searchVectorVersion;

    public static Auction of(
        @NonNull Long userId,
        String description,
        @NonNull BigDecimal maxPrice,
        @NonNull String itemName,
        @NonNull LocalDateTime startedAt,
        @NonNull LocalDateTime endedAt,
        @NonNull Long categoryId
    ) {
        Auction auction = new Auction();

        auction.userId = userId;
        auction.description = description;
        auction.maxPrice = maxPrice;
        auction.itemName = itemName;

        auction.status = AuctionStatus.READY;

        auction.startedAt = startedAt;
        auction.endedAt = endedAt;

        auction.categoryId = categoryId;

        auction.searchVectorVersion = 0;

        return auction;
    }

    public void activate() {
        if(this.status == AuctionStatus.READY) {
            this.status = AuctionStatus.ACTIVE;
        }
    }

    public void close() {
        if(this.status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.DONE;
        }
    }

    public void noBid() {
        if(this.status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.NO_BID;
        }
    }

    public void cancel() {
        if(this.status == AuctionStatus.READY) {
            this.status = AuctionStatus.CANCELLED;
            this.cancelledAt = LocalDateTime.now();
        }
    }

    public void forceCancel() {
        if (this.status == AuctionStatus.READY || this.status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.CANCELLED;
            this.cancelledAt = LocalDateTime.now();
        }
    }
}
