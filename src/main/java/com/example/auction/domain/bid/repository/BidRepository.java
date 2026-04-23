package com.example.auction.domain.bid.repository;

import com.example.auction.domain.bid.entity.Bid;
import com.example.auction.domain.bid.enums.BidAuctionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long>, BidCustomRepository {

    Page<Bid> findAllByUserId(Long userId, Pageable pageable);

    Page<Bid> findAllByAuctionId(Long auctionId, Pageable pageable);

    boolean existsByUserIdAndStatus(Long userId, BidAuctionStatus bidAuctionStatus);
}
