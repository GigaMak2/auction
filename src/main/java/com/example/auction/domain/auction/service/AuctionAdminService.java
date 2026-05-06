package com.example.auction.domain.auction.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.auction.dto.AuctionAdminListResponse;
import com.example.auction.domain.auction.dto.AuctionAdminSearchCondition;
import com.example.auction.domain.auction.entity.Auction;
import com.example.auction.domain.auction.enums.AuctionStatus;
import com.example.auction.domain.auction.exception.AuctionErrorEnum;
import com.example.auction.domain.auction.repository.AuctionRepository;
import com.example.auction.domain.auction.search.service.AuctionSearchService;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionAdminService {

    private final AuctionRepository auctionRepository;
    private final AuctionSearchService auctionSearchService;

    @Transactional(readOnly = true)
    public PageResponse<AuctionAdminListResponse> getAuctionList(AuctionAdminSearchCondition condition) {
        Page<AuctionAdminListResponse> auctionList = auctionSearchService.searchAuctionWithConditions(
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getStatus(),
                condition.getKeyword()
        );

        return PageResponse.create(auctionList);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"getManyAuctionsPublic"}, allEntries = true),
            @CacheEvict(cacheNames = {"getAuction"}, key = "#auctionId"),
    })
    public void forceCancel(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new ServiceErrorException(AuctionErrorEnum.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.READY && auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new ServiceErrorException(AuctionErrorEnum.AUCTION_STATUS_NOT_CANCELLABLE);
        }

        auction.forceCancel();
    }
}
