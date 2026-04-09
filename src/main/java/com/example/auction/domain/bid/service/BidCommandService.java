package com.example.auction.domain.bid.service;

import com.example.auction.common.exception.ServiceErrorException;
import com.example.auction.domain.bid.dto.request.BidRequest;
import com.example.auction.domain.bid.dto.response.BidResponse;
import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import com.example.auction.domain.bid.enums.BidErrorEnum;
import com.example.auction.domain.bid.repository.BidRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

// 입찰 생성
@Service
@RequiredArgsConstructor
@Slf4j
public class BidCommandService {

    private final BidRepository bidRepository;

    // 입찰 생성
    public BidResponse placeBid(AuthUser authUser, Long auctionId, @Valid BidRequest request) {
    }


}
