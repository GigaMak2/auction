package com.example.auction.domain.auction.service;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.AuctionSearchCondition;
import com.example.auction.domain.auction.dto.CreateAuctionRequest;
import com.example.auction.domain.auction.dto.GetAuctionResponse;
import com.example.auction.domain.auction.dto.GetManyAuctionsResponse;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;

    @Transactional(readOnly = true)
    public GetAuctionResponse getAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND)
        );

        return GetAuctionResponse.from(auction);
    }

    @Transactional(readOnly = true)
    public PageResponse<GetManyAuctionsResponse> getManyAuctions(
            AuctionSearchCondition condition
    ) {
        // 조회하고 싶은 status가 없는 경우 READY, ACTIVE한 경매만 조회하기
        condition.setDefaultStatusesIfEmpty(
                AuctionStatus.READY, AuctionStatus.ACTIVE
        );

        // 취소된 경매는 보여주지 말기
        if (
                condition.getStatus() != null &&
                condition.getStatus().contains(AuctionStatus.CANCELLED)
        ) {
            throw new ServiceErrorException(
                    AuctionErrorEnum.AUCTION_SEARCH_FORBIDDEN_STATUS_FILTER
            );
        }

        // 검색 조건중 최소금액이 최대 금액 보다 클경우 에러를 던지기
        if (
                condition.getMaxPriceMax() != null &&
                condition.getMaxPriceMax().compareTo(condition.getMaxPriceMin()) < 0
        ) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_SEARCH_INVLID_PRICE_RANGE);
        }

        Page<@NonNull Auction> auctions = auctionRepository.findByCondition(
                condition
        );

        Page<@NonNull GetManyAuctionsResponse> auctionsDto = auctions.map(GetManyAuctionsResponse::from);
        
        return PageResponse.create(auctionsDto);
    }

    @Transactional()
    public GetAuctionResponse createAuction(
            Long userId,
            CreateAuctionRequest req
    ) {
        // TODO: auction 값이 valid한지 check

        Auction auction = Auction.of(
                userId, 
                req.getDescription(),
                req.getMaxPrice(),
                req.getItemName(),
                req.getStartedAt(),
                req.getEndedAt(),
                req.getCategory()
        );

        auction = auctionRepository.saveAndFlush(auction);

        return GetAuctionResponse.from(auction);
    }
}
