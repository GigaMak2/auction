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

-- 테스트 유저 생성
INSERT INTO users (email, password, role, deleted, created_at, modified_at)
SELECT
    'user' || i || '@test.com',
    '$2b$10$AJg6EA0ZI8UIXOXmnFnBu.ABlb88prNJhKXeZCg8C2paX4ks6p0vS',
    'USER',
    false,
    NOW(),
    NOW()
FROM generate_series(0, 199) AS i;

-- 테스트 경매 생성
INSERT INTO auctions (user_id, item_name, description, max_price, started_at, ended_at, auction_status, category_id, created_at)
VALUES
    (1, '테스트 경매 1', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (2, '테스트 경매 2', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (3, '테스트 경매 3', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (4, '테스트 경매 4', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (5, '테스트 경매 5', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (6, '테스트 경매 6', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (7, '테스트 경매 7', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (8, '테스트 경매 8', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (9, '테스트 경매 9', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW()),
    (10, '테스트 경매 10', '테스트', 100000000, NOW() - INTERVAL '20 hour', NOW() + INTERVAL '20 hour', 'ACTIVE', 1, NOW());