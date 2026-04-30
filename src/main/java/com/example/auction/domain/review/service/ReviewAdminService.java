package com.example.auction.domain.review.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.ai.service.ReviewEmbeddingService;
import com.example.auction.domain.review.dto.ReviewAdminListResponse;
import com.example.auction.domain.review.dto.ReviewAdminSearchCondition;
import com.example.auction.domain.review.entity.Review;
import com.example.auction.domain.review.exception.ReviewErrorEnum;
import com.example.auction.domain.review.repository.ReviewRepository;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAdminService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewEmbeddingService reviewEmbeddingService;

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

    @Transactional
    public void forceDelete(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ServiceErrorException(ReviewErrorEnum.REVIEW_NOT_FOUND));

        Long revieweeId = review.getRevieweeId();

        reviewRepository.delete(review);

        Long deletedReviewId = review.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    reviewEmbeddingService.delete(deletedReviewId);
                } catch (Exception e) {
                    log.warn("[ReviewService] 임베딩 삭제 실패 — 리뷰 삭제는 정상 처리됨. reviewId={}, error={}", deletedReviewId, e.getMessage());
                }
            }
        });

        userRepository.findById(revieweeId).ifPresent(reviewee -> {
            if (!reviewee.isDeleted()) {
                Double avgScore = reviewRepository.findAvgScoreByRevieweeId(reviewee.getId());
                BigDecimal rating = avgScore != null ? BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP) : null;
                reviewee.updateRating(rating);
            }
        });
    }
}
