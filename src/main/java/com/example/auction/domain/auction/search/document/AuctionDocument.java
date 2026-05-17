package com.example.auction.domain.auction.search.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.search.dto.AuctionCreatedDocument;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
// 인덱스명 "auction-auction" - Spring 보안 정책상 "auction-" 접두사 범위만 읽기/쓰기 권한이 있음
@Document(indexName = "auction-auction")
@NoArgsConstructor
public class AuctionDocument {
    @Id
    private Long id;

    private Long userId;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String description;

    private BigDecimal maxPrice;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String itemName;

    @Field(type = FieldType.Keyword)
    private AuctionStatus status;

    @Field(type = FieldType.Date, format=DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime startedAt;

    @Field(type = FieldType.Date, format=DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime endedAt;

    @Field(type = FieldType.Date, format=DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime cancelledAt;

    private Long categoryId;

    @Field(type = FieldType.Date, format=DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime createdAt;

    public static AuctionDocument from(AuctionCreatedDocument dto) {
        AuctionDocument auctionDoc = new AuctionDocument();

        auctionDoc.id = dto.id();

        auctionDoc.userId = dto.userId();

        auctionDoc.description = dto.description();

        auctionDoc.maxPrice = dto.maxPrice();

        auctionDoc.itemName = dto.itemName();

        auctionDoc.status = dto.status();

        auctionDoc.startedAt = dto.startedAt();
        auctionDoc.endedAt = dto.endedAt();
        auctionDoc.cancelledAt = dto.cancelledAt();

        auctionDoc.categoryId = dto.categoryId();

        auctionDoc.createdAt = dto.createdAt();

        return auctionDoc;
    }

    public void setCancelled(LocalDateTime cancelledAt) {
        this.status = AuctionStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }
}

