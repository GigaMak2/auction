---------------------------
-- auction_results 
---------------------------

alter table auction_results 
    add constraint fk_auction_results_auction_id
    foreign key (auction_id) 
    REFERENCES auctions (id);

alter table auction_results 
    add constraint fk_auction_results_buyer_id
    foreign key (buyer_id) 
    REFERENCES users (id);

alter table auction_results 
    add constraint fk_auction_results_seller_id
    foreign key (seller_id) 
    REFERENCES users (id);

alter table auction_results 
    add constraint fk_auction_results_bid_id
    foreign key (bid_id) 
    REFERENCES bids (id);

---------------------------
-- auctions 
---------------------------

alter table auctions
    add constraint fk_auctions_user_id
    foreign key (user_id) 
    REFERENCES users (id);

alter table auctions
    add constraint fk_auctions_category_id
    foreign key (category_id) 
    REFERENCES categories (id);

---------------------------
-- bids 
---------------------------

alter table bids
    add constraint fk_bids_auction_id
    foreign key (auction_id) 
    REFERENCES auctions (id);

alter table bids
    add constraint fk_bids_user_id
    foreign key (user_id) 
    REFERENCES users (id);

---------------------------
-- categories 
---------------------------

alter table categories
    add constraint fk_categories_parent_id
    foreign key (parent_id) 
    REFERENCES categories (id);

---------------------------
-- chat_messages 
---------------------------

alter table chat_messages
    add constraint fk_chat_messages_room_id
    foreign key (room_id) 
    REFERENCES chat_rooms (id);

---------------------------
-- chat_rooms 
---------------------------

alter table chat_rooms
    add constraint fk_chat_rooms_user_id
    foreign key (user_id) 
    REFERENCES users (id);

---------------------------
-- reviews 
---------------------------

alter table reviews
    add constraint fk_reviews_auction_id
    foreign key (auction_id) 
    REFERENCES auctions (id);

alter table reviews
    add constraint fk_reviews_reviewer_id
    foreign key (reviewer_id) 
    REFERENCES users (id);

alter table reviews
    add constraint fk_reviews_reviewee_id
    foreign key (reviewee_id) 
    REFERENCES users (id);

---------------------------
-- user_social_accounts 
---------------------------

alter table user_social_accounts
    add constraint fk_user_social_accounts_user_id
    foreign key (user_id) 
    REFERENCES users (id);
