package com.example.auction.domain.review.repository;

import com.example.auction.domain.review.dto.ReviewListByAuctionGetResponse;
import com.example.auction.domain.review.dto.ReviewListGetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ReviewCustomRepository {
    Page<ReviewListGetResponse> findReviewsWithConditions(Long userId, Pageable pageable, LocalDate startDate, LocalDate endDate);

    List<ReviewListByAuctionGetResponse> findByAuctionId(Long auctionId);
}
