package com.example.auction.domain.auction.repository;

import com.example.auction.domain.auction.enums.AuctionStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.auction.domain.auction.entity.Auction;

import java.util.List;

public interface AuctionRepository extends
    JpaRepository<@NonNull Auction, @NonNull Long>, CustomAuctionRepository
{
    boolean existsByUserIdAndStatusIn(Long userId, List<AuctionStatus> auctionStatuses);

    List<Auction> findByUserIdAndStatusIn(Long userId, List<AuctionStatus> statuses);

    @Modifying
    @Query(value = """
UPDATE auctions SET
    item_name_search_vector = cast(:itemNameSearchVector as tsvector),
    description_search_vector = cast(:descriptionSearchVector as tsvector),
    search_vector_version = :searchVectorVersion
WHERE
    id = :id
""", nativeQuery = true)
    void updateSearchVectors(
            @Param("id") Long id, 
            @Param("itemNameSearchVector") String itemNameSearchVector,
            @Param("descriptionSearchVector") String descriptionSearchVector,
            @Param("searchVectorVersion") Integer searchVectorVersion
    );
}
