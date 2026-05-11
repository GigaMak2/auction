package com.example.auction.domain.auction.result.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.auction.result.dto.response.AuctionResultAdminListResponse;
import com.example.auction.domain.auction.result.dto.request.AuctionResultAdminPageCondition;
import com.example.auction.domain.auction.result.entity.AuctionResult;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuctionResultAdminServiceTest {

    @InjectMocks
    private AuctionResultAdminService auctionResultAdminService;

    @Mock
    private AuctionResultRepository auctionResultRepository;


    // ========================
    // 경매 결과 목록 조회
    // ========================

    @Test
    @DisplayName("경매 결과 목록 조회 성공")
    void getAuctionResultList_success() {
        // given
        AuctionResultAdminPageCondition condition = new AuctionResultAdminPageCondition();

        AuctionResult result1 = AuctionResult.of(BigDecimal.valueOf(10000), 1L, 1L, 2L, 1L);
        ReflectionTestUtils.setField(result1, "id", 1L);
        ReflectionTestUtils.setField(result1, "createdAt", LocalDateTime.now());

        AuctionResult result2 = AuctionResult.of(BigDecimal.valueOf(20000), 2L, 3L, 4L, 2L);
        ReflectionTestUtils.setField(result2, "id", 2L);
        ReflectionTestUtils.setField(result2, "createdAt", LocalDateTime.now());

        Page<AuctionResult> page = new PageImpl<>(List.of(result1, result2), PageRequest.of(0, 20), 2);
        given(auctionResultRepository.findAll(any(Pageable.class))).willReturn(page);

        // when
        PageResponse<AuctionResultAdminListResponse> response = auctionResultAdminService.getAuctionResultList(condition);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.isLast()).isTrue();
        assertThat(response.content().get(0).auctionId()).isEqualTo(1L);
        assertThat(response.content().get(0).price()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(response.content().get(1).auctionId()).isEqualTo(2L);
        assertThat(response.content().get(1).price()).isEqualByComparingTo(BigDecimal.valueOf(20000));
    }

    @Test
    @DisplayName("경매 결과 목록 조회 성공 - 빈 리스트")
    void getAuctionResultList_success_empty() {
        // given
        AuctionResultAdminPageCondition condition = new AuctionResultAdminPageCondition();
        Page<AuctionResult> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        given(auctionResultRepository.findAll(any(Pageable.class))).willReturn(page);

        // when
        PageResponse<AuctionResultAdminListResponse> response = auctionResultAdminService.getAuctionResultList(condition);

        // then
        assertThat(response.content()).hasSize(0);
        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.isLast()).isTrue();
    }
}