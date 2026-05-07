// 실제 서비스 부하 확인
// 90명 동시 입찰 30초간
import http from 'k6/http';
import { Rate } from 'k6/metrics';
import { check, sleep } from 'k6';
import { setup, authHeaders, BUYER_COUNT } from './common.js';

export { setup };

export const options = {
    vus: BUYER_COUNT,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% 요청이 500ms 이내이고,
        business_success: ['rate>0.99'],
    },
};

const BASE_URL = 'http://host.docker.internal:8080';
const businessSuccess = new Rate('business_success');

export default function (data) {
    // 유저를 각 경매에 분산
    const token = data.buyerTokens[__VU % data.buyerTokens.length];
    const auctionId = data.auctionIds[__VU % data.auctionIds.length];

    // 현재 최저가 조회
    const currentRes = http.get(
        `${BASE_URL}/api/auctions/${auctionId}/bids/current/v1`,
        authHeaders(token)
    );
    let minPrice = 10000000;
    if (currentRes.status === 200) {
        minPrice = currentRes.json('data.price');
    }

    // 최저가보다 1~100 낮은 가격으로 입찰 시도
    let price = minPrice - Math.floor(Math.random() * 100) - 1;
    if (price <= 0) {
        price = Math.floor(Math.random() * 100) + 1;
        sleep(1);}

    // 입찰
    const params = {
        ...authHeaders(token),
        responseCallback: http.expectedStatuses(201, 400),
    };

    const res = http.post(
        `${BASE_URL}/api/auctions/${auctionId}/bids/v2`,
        JSON.stringify({ price }),
        params
    );

    businessSuccess.add(res.status === 201 || res.status === 400);

    check(res, {
        'status is 201 or 400': (r) => r.status === 201 || r.status === 400,
        'no server error': (r) => r.status < 500,
    });

    sleep(1);
}