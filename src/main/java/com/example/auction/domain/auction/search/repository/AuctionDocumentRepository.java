package com.example.auction.domain.auction.search.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.example.auction.domain.auction.search.document.AuctionDocument;

interface AuctionDocumentRepository extends ElasticsearchRepository<AuctionDocument, Long> {}
