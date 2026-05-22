package com.example.auction.domain.auction.search.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionDocumentUtil {
    // 인덱스명 "auction-auction" - Spring 보안 정책상 "auction-" 접두사 범위만 읽기/쓰기 권한이 있음
    public static final String ALIAS_NAME = "auction-auction";

    public static String getNewIndexName(LocalDateTime beganTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("-yyyy-MM-dd-HH-mm-ss");
        String dateFormat = beganTime.format(formatter);

        return ALIAS_NAME + dateFormat;
    }
}
