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

    // =======================================
    //
    // 아래는 JDBC를 통해 관리되는 column들입니다.
    // 
    // 검색은 tsvector를 통해 동작하는데 Hibernate를 통해
    // 깔끔하게 tsvector를 관리할 방법이 없으므로 JDBC를 통해 관리합니다.
    //
    // 아래는 QueryDSL path를 위해서만 존재합니다.

    @Column(columnDefinition = "tsvector", insertable = false, updatable = false)
    private String itemNameSearchVector;

    @Column(columnDefinition = "tsvector", insertable = false, updatable = false)
    private String descriptionSearchVector;

    // =======================================

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

    // auction 상태 변경 메서드들
    // READY -> ACTIVE (경매 시작)
    public void activate() {
        if(this.status == AuctionStatus.READY) {
            this.status = AuctionStatus.ACTIVE;
        }
    }

    // ACTIVE -> DONE (낙찰)
    public void close() {
        if(this.status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.DONE;
        }
    }

    // ACTIVE -> NO_BID (유찰)
    public void noBid() {
        if(this.status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.NO_BID;
        }
    }

    // READY -> CANCELLED (취소)
    public void cancel() {
        if(this.status == AuctionStatus.READY) {
            this.status = AuctionStatus.CANCELLED;
            this.cancelledAt = LocalDateTime.now();
        }
    }

    // 관리자 전용 강제 취소
    public void forceCancel() {
        if (this.status == AuctionStatus.READY || this.status == AuctionStatus.ACTIVE) {
            this.status = AuctionStatus.CANCELLED;
            this.cancelledAt = LocalDateTime.now();
        }
    }
}
