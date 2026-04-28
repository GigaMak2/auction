package com.example.auction.domain.auction.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.auction.dto.AuctionAdminSearchCondition;
import com.example.auction.domain.auction.dto.GetManyAuctionsResponse;
import com.example.auction.domain.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionAdminService {

    private final AuctionRepository auctionRepository;

    @Transactional(readOnly = true)
    public PageResponse<GetManyAuctionsResponse> getAuctionList(AuctionAdminSearchCondition condition) {
        Page<GetManyAuctionsResponse> auctionList = auctionRepository.findAuctionWithConditions(
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getAuctionStatus(),
                condition.getKeyword()
        );

        return PageResponse.create(auctionList);
    }
}
