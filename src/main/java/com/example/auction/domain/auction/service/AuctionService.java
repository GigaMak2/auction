package com.example.auction.domain.auction.service;

import java.time.Duration;
import java.time.LocalDateTime;

import com.example.auction.domain.auction.eventbridge.entity.AuctionCancelledEventBridge;
import com.example.auction.domain.auction.eventbridge.entity.AuctionCreatedEventBridge;
import com.example.auction.domain.auction.eventbridge.entity.AuctionScheduleOutbox;
import com.example.auction.domain.auction.eventbridge.repository.AuctionScheduleOutboxRepository;
import com.example.auction.domain.category.exception.CategoryErrorEnum;
import com.example.auction.domain.category.repository.CategoryRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.auction.common.config.security.CustomUserDetails;
import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.request.AuctionSearchCondition;
import com.example.auction.domain.auction.dto.request.CreateAuctionRequest;
import com.example.auction.domain.auction.dto.response.GetAuctionResponse;
import com.example.auction.domain.auction.dto.response.GetManyAuctionsResponse;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.search.dto.AuctionCreatedDocument;
import com.example.auction.domain.auction.search.dto.AuctionSearchResult;
import com.example.auction.domain.auction.search.service.AuctionSearchService;
import com.example.auction.domain.auction.util.AuctionUtil;
import com.example.auction.domain.user.exception.UserErrorEnum;
import com.example.auction.domain.user.repository.UserRepository;
import com.example.auction.domain.auction.search.util.KoreanAnalyzerUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuctionScheduleOutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuctionSearchService auctionSearchService;
    private final KoreanAnalyzerUtil koreanAnalyzerUtil;

    @Transactional(readOnly = true)
    @Cacheable(
        cacheNames =  {"getAuction"},
        key = "#auctionId"
    )
    public GetAuctionResponse getAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND)
        );

        return GetAuctionResponse.from(auction);
    }

    @Cacheable(
        cacheNames = {"getManyAuctionsPublic"},
        key = "@auctionCacheService.getManyAuctionsPublicCacheKey(#condition)",
        condition = "@auctionCacheService.shouldCacheGetManyAuctionsPublic(#condition)"
    )
    public PageResponse<GetManyAuctionsResponse> getManyAuctionsPublic (
            AuctionSearchCondition condition
    ) {
        condition.setDefaultStatusesIfEmpty(
                AuctionStatus.READY, AuctionStatus.ACTIVE
        );

        if (
                condition.getStatus() != null &&
                condition.getStatus().contains(AuctionStatus.CANCELLED)
        ) {
            throw new ServiceErrorException(
                    AuctionErrorEnum.AUCTION_SEARCH_FORBIDDEN_STATUS_FILTER
            );
        }

        AuctionUtil.throwIfSearchConditionNotValid(condition);

        Page<AuctionSearchResult> searchResults = auctionSearchService.searchAuction(condition);

        Page<GetManyAuctionsResponse> dtos = searchResults.map(GetManyAuctionsResponse::from);
        
        return PageResponse.create(dtos);
    }

    @Transactional(readOnly = true)
    public PageResponse<GetManyAuctionsResponse> getManyAuctionsMe (
            CustomUserDetails userDetails,
            AuctionSearchCondition condition
    ) {
        AuctionUtil.throwIfSearchConditionNotValid(condition);

        Page<AuctionSearchResult> searchResults = auctionSearchService.searchAuction(
                userDetails.getUserId(), condition
        );

        Page<GetManyAuctionsResponse> auctionsDto = searchResults.map(GetManyAuctionsResponse::from);
        
        return PageResponse.create(auctionsDto);
    }

    @Transactional
    @CachePut(
        cacheNames = {"getAuction"},
        key = "#result.getId()"
    )
    @CacheEvict(
        cacheNames = {"getManyAuctionsPublic"},
        allEntries = true
    )
    public GetAuctionResponse createAuction(
            CustomUserDetails userDetails,
            CreateAuctionRequest req
    ) {
        userRepository.findByIdAndDeletedFalse(userDetails.getUserId()).orElseThrow(()->
            new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND)
        );

        AuctionUtil.throwIfCreateAuctionRequestNotValid(req);

        categoryRepository.findById(req.getCategoryId()).orElseThrow(() ->
                new ServiceErrorException(CategoryErrorEnum.CATEGORY_NOT_FOUND));

        Auction auction = Auction.of(
                userDetails.getUserId(), 
                req.getDescription(),
                req.getMaxPrice(),
                req.getItemName(),
                req.getStartedAt(),
                req.getEndedAt(),
                req.getCategoryId()
        );

        auction = auctionRepository.saveAndFlush(auction);

        outboxRepository.save(AuctionScheduleOutbox.of(auction.getId(), "START", auction.getStartedAt()));
        outboxRepository.save(AuctionScheduleOutbox.of(auction.getId(), "END", auction.getEndedAt()));

        // 주의: 반드시 saveAndFlush 이후에 일어나야 함
        String itemNameVector = koreanAnalyzerUtil.toTsVectorLiteral(auction.getItemName());
        String descriptionVector = koreanAnalyzerUtil.toTsVectorLiteral(auction.getDescription());

        auctionRepository.updateSearchVectors(
                auction.getId(), itemNameVector, descriptionVector, 1
        );

        // 트랜잭션 커밋 이후 EventBridge 등록 (고아 스케줄 방지)
        // AuctionEventBridgeService의 handleAuctionCreated() 호출
        eventPublisher.publishEvent(new AuctionCreatedEventBridge(auction.getId(), auction.getStartedAt(), auction.getEndedAt()));

        // AuctionSearchService의 handleAuctionCreated() 호출
        eventPublisher.publishEvent(AuctionCreatedDocument.from(auction));

        return GetAuctionResponse.from(auction);
    }

    @Transactional
    @Caching(
        evict = {
            @CacheEvict(
                cacheNames = {"getManyAuctionsPublic"},
                allEntries = true
            ),
            @CacheEvict(
                cacheNames = {"getAuction"},
                key = "#auctionId"
            ),
        }
    )
    public void cancelAuction(
            Long auctionId,
            CustomUserDetails details
    ) {
        // 경매를 취소할 때 비관적 락을 걸지 않고 select
        // 경매 시작 직전에 취소를 막기 때문에 동시성 문제가 발생할 일이 없고 여러 사람이 같은 경매를 취소 할 일이 없음
        LocalDateTime now = LocalDateTime.now();

        userRepository.findByIdAndDeletedFalse(details.getUserId()).orElseThrow(()->
            new ServiceErrorException(UserErrorEnum.USER_NOT_FOUND)
        );

        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND)
        );

        if (!auction.getUserId().equals(details.getUserId())) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_FORBIDDEN_FROM_CANCEL);
        }

        if (auction.getStatus().equals(AuctionStatus.CANCELLED)) {
            return;
        }

        if (!auction.getStatus().equals(AuctionStatus.READY)) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_STATUS_NOT_CANCELLABLE);
        }

        LocalDateTime canCancelAfter = auction.getStartedAt().minus(Duration.ofMinutes(10));
        if (!now.isBefore(canCancelAfter)) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_TOO_LATE_TO_CANCEL);
        }

        auction.cancel();

        auctionRepository.saveAndFlush(auction);
        // auctionEventBridgeService의 handleAuctionCancelled() 호출
        eventPublisher.publishEvent(new AuctionCancelledEventBridge(auction.getId()));
    }
}
