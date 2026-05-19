package com.example.auction.domain.auction.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.auction.domain.auction.search.entity.AuctionDocumentReindexJob;


public interface AuctionDocumentReindexJobRepository extends JpaRepository<AuctionDocumentReindexJob, Long> {}
