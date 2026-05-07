// 시스템 한계점 찾기
// vu를 단계적으로 올려서 어느 시점에 에러가 터지거나 급격히 응답시간이 늘어나는지 확인
import http from 'k6/http';
import { Rate } from 'k6/metrics';
import { check, sleep } from 'k6';
import { setup, authHeaders } from './common.js';

export { setup };

export const options = {
    stages: [
        { duration: '10s', target: 100 },
        { duration: '30s', target: 300 },
        { duration: '30s', target: 500 },
        { duration: '30s', target: 700 },
        { duration: '30s', target: 900 },
        { duration: '30s', target: 1100 },
        { duration: '30s', target: 1300 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        business_success: ['rate>0.8'],
    },
};

const BASE_URL = 'http://host.docker.internal:8080';
const businessSuccess = new Rate('business_success');

export default function (data) {
    // 사용자를 각 경매에 분산
    const token = data.buyerTokens[__VU % data.buyerTokens.length];
    const auctionId = data.auctionIds[__VU % data.auctionIds.length];

    // 현재 최저가 조회하여 있으면 그거 사용, 없으면 기본값
    const currentRes = http.get(
        `${BASE_URL}/api/auctions/${auctionId}/bids/current/v1`,
        authHeaders(token)
    );

    let minPrice = 10000000;
    if (currentRes.status === 200) {
        minPrice = currentRes.json('data.price');
    }

    // 입찰 가격 정하기(현재 최저가보다 1~10 낮은 가격)
    let price = minPrice - Math.floor(Math.random() * 10) - 1;
    if (price <= 0) {
        return;
    }

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

}