package com.example.auction.domain.auction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.auction.domain.auction.entity.Auction;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
