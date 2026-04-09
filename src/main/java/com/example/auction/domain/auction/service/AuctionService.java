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
            AuctionSearchCondition conditionDto
    ) {
        // TODO: conditionDto 값이 valid한지 check

        Page<@NonNull Auction> auctions = auctionRepository.findByCondition(
                conditionDto
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
