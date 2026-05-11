
ALTER TABLE bids
    ADD CONSTRAINT uq_bids_auction_price UNIQUE (auction_id, price);

ALTER TABLE auction_results
    ADD CONSTRAINT uq_auction_results_auction_id UNIQUE (auction_id);


