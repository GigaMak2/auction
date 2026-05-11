package com.example.auction.domain.bid.service;

import com.example.auction.domain.bid.dto.response.BidCachedResponse;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BidCacheService {

    private final BidRepository bidRepository;

    @Cacheable(value = "currentMinBid", key = "#auctionId")
    public BidCachedResponse getCurrentMinPrice(Long auctionId) {
        return bidRepository.findFirstByAuctionIdOrderByPriceAsc(auctionId)
                .map(BidCachedResponse::of)
                .orElse(null);
    }
}
