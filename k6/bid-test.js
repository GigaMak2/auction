import http from 'k6/http';
import { check, sleep } from 'k6';
import { setup, authHeaders, BUYER_COUNT } from './common.js';

export { setup };

export const options = {
    vus: BUYER_COUNT,
    duration: '30s',
};

const BASE_URL = 'http://host.docker.internal:8080';

export default function (data) {
    const token = data.buyerTokens[__VU % data.buyerTokens.length];
    const auctionId = data.auctionIds[__VU % data.auctionIds.length];

    // 1. 현재 최저가 조회
    const currentRes = http.get(
        `${BASE_URL}/api/auctions/${auctionId}/bids/current/v1`,
        authHeaders(token)
    );

    let minPrice = 100000;
    if (currentRes.status === 200) {
        minPrice = currentRes.json('data.price');
    }

    // 2. 현재 최저가보다 낮은 가격으로 입찰
    const price = minPrice - Math.floor(Math.random() * 100000) - 1;
    if (price <= 0) {
        sleep(1);
        return;
    }

    const res = http.post(
        `${BASE_URL}/api/auctions/${auctionId}/bids/v2`,
        JSON.stringify({ price }),
        authHeaders(token)
    );

    check(res, {
        'status is 201': (r) => r.status === 201,
    });

 //   console.log(res.status, res.body);
    sleep(1);
}