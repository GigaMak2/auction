CREATE UNIQUE INDEX idx_unique_root_category_name
ON categories (name)
WHERE parent_id IS NULL;
