package com.example.auction.domain.bid.repository;

import com.example.auction.domain.bid.entity.Bid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long>, BidCustomRepository {
    Optional<Bid> findWinnerBidByAuctionId(Long auctionId);

    Page<Bid> findAllByUserId(Long userId, Pageable pageable);

    Page<Bid> findAllByAuctionId(Long auctionId, Pageable pageable);
}
