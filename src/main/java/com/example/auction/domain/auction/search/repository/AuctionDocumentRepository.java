package com.example.auction.domain.auction.search.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.auction.domain.auction.search.document.AuctionDocument;

public interface AuctionDocumentRepository extends ElasticsearchRepository<AuctionDocument, Long> {}
