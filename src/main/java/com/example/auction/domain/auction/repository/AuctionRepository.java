package com.example.auction.domain.auction.repository;

import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.auction.domain.auction.entity.Auction;

public interface AuctionRepository extends
    JpaRepository<@NonNull Auction, @NonNull Long>, CustomAuctionRepository
{
}
