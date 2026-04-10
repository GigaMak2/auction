package com.example.auction.domain.review.entity;

import com.example.auction.common.entity.ModifiableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long reviewerId;

    @Column(nullable = false)
    private Long revieweeId;

    @Column(nullable = false)
    private int score;

    private String description;

    public static Review of(Long auctionId, Long reviewerId, Long revieweeId, int score, String description) {
        Review review = new Review();
        review.auctionId = auctionId;
        review.reviewerId = reviewerId;
        review.revieweeId = revieweeId;
        review.score = score;
        review.description = description;

        return review;
    }
}
