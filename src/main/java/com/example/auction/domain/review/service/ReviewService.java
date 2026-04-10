package com.example.auction.domain.review.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.review.dto.ReviewCreateRequest;
import com.example.auction.domain.review.dto.ReviewCreateResponse;
import com.example.auction.domain.review.entity.Review;
import com.example.auction.domain.review.exception.ReviewErrorEnum;
import com.example.auction.domain.review.repository.ReviewRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewCreateResponse createReview(Long userId, ReviewCreateRequest request) {
        // TODO auctionId로 경매 결과 조회
        // TODO 리뷰 작성자가 구매자인지 판매자인지 확인 후 revieweeId 결정
        /*
        Long revieweeId;
        if (auctionResult.getBuyerId.equals(userId)) {
            revieweeId = actionResult.getSellerId;
        } else if (auctionResult.getSellerId.equals(userId)) {
            revieweeId = auctionResult.getBuyerId;
        } else {
            throw new 예외 던지기
        }
         */

        if (reviewRepository.existsByAuctionIdAndReviewerId(request.auctionId(), userId)) {
            throw new ServiceErrorException(ReviewErrorEnum.ALREADY_REVIEWED);
        }

        // TODO 경매 결과 연동되면 request.revieweeId() → revieweeId 로 수정
        User reviewee = userRepository.findByIdAndDeletedFalse(request.revieweeId()).orElseThrow(
                () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

        Review review = Review.of(request.auctionId(), userId, reviewee.getId(), request.score(), request.description());
        reviewRepository.save(review);

        Double avgScore = reviewRepository.findAvgScoreByRevieweeId(reviewee.getId());
        BigDecimal rating = BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP);
        reviewee.updateRating(rating);

        return new ReviewCreateResponse(
                review.getId(),
                review.getAuctionId(),
                review.getReviewerId(),
                review.getRevieweeId(),
                review.getScore(),
                review.getDescription(),
                review.getCreatedAt()
        );
    }
}
