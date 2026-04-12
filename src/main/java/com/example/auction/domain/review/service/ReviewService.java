package com.example.auction.domain.review.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.review.dto.*;
import com.example.auction.domain.review.entity.Review;
import com.example.auction.domain.review.exception.ReviewErrorEnum;
import com.example.auction.domain.review.repository.ReviewRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

    @Transactional(readOnly = true)
    public PageResponse<ReviewListGetResponse> getReviewList(Long userId, ReviewSearchCondition condition) {
        Page<ReviewListGetResponse> reviewList = reviewRepository.findReviewsWithConditions(
                userId,
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getStartDate(),
                condition.getEndDate()
        );

        return PageResponse.create(reviewList);
    }

    @Transactional(readOnly = true)
    public ReviewGetResponse getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ServiceErrorException(ReviewErrorEnum.REVIEW_NOT_FOUND));

        return new ReviewGetResponse(
                review.getId(),
                review.getAuctionId(),
                review.getReviewerId(),
                review.getRevieweeId(),
                review.getScore(),
                review.getDescription(),
                review.getCreatedAt(),
                review.getModifiedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewListByAuctionGetResponse> getReviewListByAuction(Long auctionId) {
        return reviewRepository.findByAuctionId(auctionId);
    }

    @Transactional
    public ReviewModifyResponse modifyReview(Long userId, Long reviewId, ReviewModifyRequest request) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ServiceErrorException(ReviewErrorEnum.REVIEW_NOT_FOUND));

        if (!review.getReviewerId().equals(userId)) {
            throw new ServiceErrorException(ReviewErrorEnum.REVIEW_FORBIDDEN);
        }

        review.modify(request);

        if (request.score() != null) {
            User reviewee = userRepository.findByIdAndDeletedFalse(review.getRevieweeId()).orElseThrow(
                    () -> new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND));

            Double avgScore = reviewRepository.findAvgScoreByRevieweeId(reviewee.getId());
            BigDecimal rating = BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP);
            reviewee.updateRating(rating);
        }

        return new ReviewModifyResponse(
                review.getId(),
                review.getScore(),
                review.getDescription(),
                review.getCreatedAt(),
                review.getModifiedAt()
        );
    }
}
