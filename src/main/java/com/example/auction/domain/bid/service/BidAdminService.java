package com.example.auction.domain.bid.service;

import com.example.auction.common.dto.PageResponse;
import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.bid.dto.request.BidAdminSearchCondition;
import com.example.auction.domain.bid.dto.response.BidAdminListResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.exceptions.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BidAdminService {

    private final BidRepository bidRepository;

    @Transactional(readOnly = true)
    public PageResponse<BidAdminListResponse> getBidList(BidAdminSearchCondition condition) {
        Page<BidAdminListResponse> bidList = bidRepository.findBidWithConditions(
                PageRequest.of(condition.getPage(), condition.getSize()),
                condition.getStatus(),
                condition.getAuctionId(),
                condition.getUserId()
        );

        return PageResponse.create(bidList);
    }

    @Transactional
    public void forceCancel(Long bidId) {
        Bid bid = bidRepository.findById(bidId).orElseThrow(
                () -> new ServiceErrorException(BidErrorEnum.BID_NOT_FOUND));

        if (bid.getStatus() != BidAuctionStatus.ACTIVE) {
            throw new ServiceErrorException(BidErrorEnum.BID_STATUS_NOT_CANCELLABLE);
        }

        bid.updateStatus(BidAuctionStatus.CANCELLED);
    }
}
