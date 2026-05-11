package com.example.auction.domain.auction.result.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.domain.auction.result.dto.response.AuctionResultAdminListResponse;
import com.example.auction.domain.auction.result.dto.request.AuctionResultAdminPageCondition;
import com.example.auction.domain.auction.result.repository.AuctionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionResultAdminService {

    private final AuctionResultRepository auctionResultRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuctionResultAdminListResponse> getAuctionResultList(AuctionResultAdminPageCondition condition) {
        Page<AuctionResultAdminListResponse> auctionResultList = auctionResultRepository
                .findAll(PageRequest.of(condition.getPage(), condition.getSize(), Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(AuctionResultAdminListResponse::from);

        return PageResponse.create(auctionResultList);
    }
}
