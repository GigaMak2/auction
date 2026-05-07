ALTER TABLE auctions
ADD item_name_search_vector tsvector;

ALTER TABLE auctions
ADD description_search_vector tsvector;

ALTER TABLE auctions
ADD search_vector_version int not null default 0;

CREATE INDEX auctions_item_name_search_vector_idx ON auctions USING GIN (item_name_search_vector);
CREATE INDEX auctions_description_search_vector_idx ON auctions USING GIN (description_search_vector);
