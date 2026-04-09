package com.example.auction.domain.auction.service;

import org.springframework.stereotype.Service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.CreateAuctionRequest;
import com.example.auction.domain.auction.dto.GetAuctionResponse;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {
    private final AuctionRepository auctionRepository;

    public GetAuctionResponse getAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND)
        );

        return GetAuctionResponse.from(auction);
    }

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
