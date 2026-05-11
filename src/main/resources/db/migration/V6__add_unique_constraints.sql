
DELETE FROM bids
WHERE id NOT IN (
    SELECT MIN(id) FROM bids GROUP BY auction_id, price
);

ALTER TABLE bids
    ADD CONSTRAINT uq_bids_auction_price UNIQUE (auction_id, price);

DELETE FROM auction_results
WHERE id NOT IN (
    SELECT MIN(id) FROM auction_results GROUP BY auction_id
);

ALTER TABLE auction_results
    ADD CONSTRAINT uq_auction_results_auction_id UNIQUE (auction_id);


