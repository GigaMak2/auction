// 최소한의 부하에서 정상 동작 확인
// VU 5명
import http from 'k6/http';
import { Rate } from 'k6/metrics';
import { check, sleep } from 'k6';
import { setup, authHeaders } from './common.js';

export { setup };

export const options = {
    vus: 5,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500'],
        business_success: ['rate>0.99'],
    },
};

const BASE_URL = 'http://host.docker.internal:8080';
const businessSuccess = new Rate('business_success');

export default function (data) {
    const token = data.buyerTokens[__VU % data.buyerTokens.length];
    const auctionId = data.auctionIds[__VU % data.auctionIds.length];


    const currentRes = http.get(
        `${BASE_URL}/api/auctions/${auctionId}/bids/current/v1`,
        authHeaders(token)
    );

    let minPrice = 10000000;
    if (currentRes.status === 200) {
        minPrice = currentRes.json('data.price');
    }

    let price = minPrice - Math.floor(Math.random() * 100) - 1;
    if (price <= 0) {
        price = Math.floor(Math.random() * 100) + 1;
        sleep(1); }

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