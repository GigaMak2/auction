-- 루트 카테고리
INSERT INTO categories (name, parent_id, depth, created_at, modified_at) VALUES
('전자기기', NULL, 0, NOW(), NOW()),
('가구',    NULL, 0, NOW(), NOW()),
('의류',    NULL, 0, NOW(), NOW()),
('도서',    NULL, 0, NOW(), NOW()),
('수집품',  NULL, 0, NOW(), NOW()),
('장난감',  NULL, 0, NOW(), NOW()),
('기타',    NULL, 0, NOW(), NOW());

-- 전자기기 하위
INSERT INTO categories (name, parent_id, depth, created_at, modified_at)
SELECT name, (SELECT id FROM categories WHERE name = '전자기기'), 1, NOW(), NOW()
FROM (VALUES ('스마트폰'), ('노트북')) AS t(name);

-- 가구 하위
INSERT INTO categories (name, parent_id, depth, created_at, modified_at)
SELECT name, (SELECT id FROM categories WHERE name = '가구'), 1, NOW(), NOW()
FROM (VALUES ('소파'), ('책상')) AS t(name);

-- 의류 하위
INSERT INTO categories (name, parent_id, depth, created_at, modified_at)
SELECT name, (SELECT id FROM categories WHERE name = '의류'), 1, NOW(), NOW()
FROM (VALUES ('상의'), ('하의')) AS t(name);

-- 스마트폰 하위
INSERT INTO categories (name, parent_id, depth, created_at, modified_at)
SELECT name, (SELECT id FROM categories WHERE name = '스마트폰'), 2, NOW(), NOW()
FROM (VALUES ('아이폰'), ('갤럭시')) AS t(name);
