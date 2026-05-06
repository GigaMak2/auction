package com.example.auction.domain.review.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.ai.service.ReviewEmbeddingService;
import com.example.auction.domain.review.dto.ReviewAdminListResponse;
import com.example.auction.domain.review.dto.ReviewAdminSearchCondition;
import com.example.auction.domain.review.entity.Review;
import com.example.auction.domain.review.exception.ReviewErrorEnum;
import com.example.auction.domain.review.repository.ReviewRepository;
import com.example.auction.domain.user.entity.User;
import com.example.auction.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewAdminServiceTest {

    @InjectMocks
    private ReviewAdminService reviewAdminService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewEmbeddingService reviewEmbeddingService;

    private MockedStatic<TransactionSynchronizationManager> mockedTsm;

    @BeforeEach
    void setUpTransactionSync() {
        mockedTsm = mockStatic(TransactionSynchronizationManager.class);
    }

    @AfterEach
    void tearDownTransactionSync() {
        mockedTsm.close();
    }


    // ========================
    // 리뷰 목록 조회
    // ========================

    @Test
    @DisplayName("리뷰 목록 조회 성공")
    void getReviewList_success() {
        // given
        ReviewAdminSearchCondition condition = new ReviewAdminSearchCondition();
        LocalDateTime now = LocalDateTime.now();

        List<ReviewAdminListResponse> reviewList = List.of(
                new ReviewAdminListResponse(1L, 10L, 1L, 2L, 5, now),
                new ReviewAdminListResponse(2L, 11L, 3L, 4L, 3, now)
        );
        Page<ReviewAdminListResponse> page = new PageImpl<>(reviewList, PageRequest.of(0, 20), 2);

        given(reviewRepository.findReviewsWithConditions(any(Pageable.class), any(), any(), any(), any())).willReturn(page);

        // when
        PageResponse<ReviewAdminListResponse> response = reviewAdminService.getReviewList(condition);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - 빈 리스트")
    void getReviewList_success_empty() {
        // given
        ReviewAdminSearchCondition condition = new ReviewAdminSearchCondition();
        Page<ReviewAdminListResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        given(reviewRepository.findReviewsWithConditions(any(Pageable.class), any(), any(), any(), any())).willReturn(page);

        // when
        PageResponse<ReviewAdminListResponse> response = reviewAdminService.getReviewList(condition);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.isLast()).isTrue();
    }


    // ========================
    // 리뷰 강제 삭제
    // ========================

    @Test
    @DisplayName("리뷰 강제 삭제 성공")
    void forceDelete_success() {
        // given
        Long reviewId = 1L;
        Review review = Review.of(10L, 1L, 2L, 5, "좋아요", null);
        ReflectionTestUtils.setField(review, "id", reviewId);

        User reviewee = User.of("seller@test.com", "encodedPassword");
        ReflectionTestUtils.setField(reviewee, "id", 2L);
        ReflectionTestUtils.setField(reviewee, "rating", BigDecimal.valueOf(5.0));

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(userRepository.findById(2L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.findAvgScoreByRevieweeId(2L)).willReturn(4.0);

        // when
        reviewAdminService.forceDelete(reviewId);

        // then
        verify(reviewRepository).delete(review);
        assertThat(reviewee.getRating()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
    }

    @Test
    @DisplayName("리뷰 강제 삭제 성공 - 마지막 리뷰 삭제 시 평점 null")
    void forceDelete_success_lastReview() {
        // given
        Long reviewId = 1L;
        Review review = Review.of(10L, 1L, 2L, 5, "좋아요", null);
        ReflectionTestUtils.setField(review, "id", reviewId);

        User reviewee = User.of("seller@test.com", "encodedPassword");
        ReflectionTestUtils.setField(reviewee, "id", 2L);
        ReflectionTestUtils.setField(reviewee, "rating", BigDecimal.valueOf(5.0));

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(userRepository.findById(2L)).willReturn(Optional.of(reviewee));
        given(reviewRepository.findAvgScoreByRevieweeId(2L)).willReturn(null);

        // when
        reviewAdminService.forceDelete(reviewId);

        // then
        verify(reviewRepository).delete(review);
        assertThat(reviewee.getRating()).isNull();
    }

    @Test
    @DisplayName("리뷰 강제 삭제 성공 - 리뷰 대상자 탈퇴 시 평점 업데이트 스킵")
    void forceDelete_success_revieweeDeleted() {
        // given
        Long reviewId = 1L;
        Review review = Review.of(10L, 1L, 2L, 5, "좋아요", null);
        ReflectionTestUtils.setField(review, "id", reviewId);

        User reviewee = User.of("seller@test.com", "encodedPassword");
        ReflectionTestUtils.setField(reviewee, "id", 2L);
        ReflectionTestUtils.setField(reviewee, "deleted", true);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
        given(userRepository.findById(2L)).willReturn(Optional.of(reviewee));

        // when
        reviewAdminService.forceDelete(reviewId);

        // then
        verify(reviewRepository).delete(review);
        verify(reviewRepository, never()).findAvgScoreByRevieweeId(any());
    }

    @Test
    @DisplayName("리뷰 강제 삭제 실패 - 리뷰 없음")
    void forceDelete_fail_reviewNotFound() {
        // given
        Long reviewId = 1L;
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewAdminService.forceDelete(reviewId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(ReviewErrorEnum.REVIEW_NOT_FOUND.getMessage());
    }
}