import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = 'http://host.docker.internal:8080';
const USER_COUNT = 200;
const SELLER_COUNT = Math.floor(USER_COUNT * 0.1);
export const BUYER_COUNT = USER_COUNT - SELLER_COUNT;

export function setup() {
    const sellerTokens = [];
    const buyerTokens = [];

    // data.sql에 있는 데이터로 로그인
    for (let i = 0; i < USER_COUNT; i++) {
        const email = `user${i}@test.com`;
        const password = '1234567890';

        // 로그인
        const res = http.post(`${BASE_URL}/api/auth/login`,
            JSON.stringify({ email, password }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        check(res, { 'login success': (r) => r.status === 200 });
        const token = res.json('data.accessToken');

        // 전체 인원의 10% 판매자로 가정
        if (i < SELLER_COUNT) {
            sellerTokens.push(token);
        } else {
            buyerTokens.push(token);
        }
    }


    return { buyerTokens, auctionIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] };

}

export function authHeaders(token) {
    return {
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
        },
    };
}