package com.example.auction.domain.bid.repository;

import com.example.auction.domain.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long>, BidCustomRepository {
    Optional<Bid> findWinnerBidByAuctionId(Long auctionId);
}
