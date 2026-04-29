package com.example.auction.domain.review.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.review.dto.ReviewAdminListResponse;
import com.example.auction.domain.review.dto.ReviewAdminSearchCondition;
import com.example.auction.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewAdminService {

    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public PageResponse<ReviewAdminListResponse> getReviewList(ReviewAdminSearchCondition condition) {
        Page<ReviewAdminListResponse> reviewList = reviewRepository.findReviewsWithConditions(
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getAuctionId(),
                condition.getReviewerId(),
                condition.getRevieweeId(),
                condition.getScore()
        );

        return PageResponse.create(reviewList);
    }
}
