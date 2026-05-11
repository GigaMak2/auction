// v2용
import http from 'k6/http';
import { Counter, Rate } from 'k6/metrics';
import { check, sleep } from 'k6';
import { setup, authHeaders } from './common.js';

export { setup };

const BASE_URL = 'http://host.docker.internal:8080';
const successCount = new Counter('success_count');
const failCount = new Counter('fail_count');
const businessSuccessRate = new Rate('business_success_rate');

export const options = {
    scenarios: {
        concurrency_compare: {
            executor: 'constant-vus',
            vus: 1300,
            duration: '30s',
            gracefulStop: '60s',
        },
    },
    thresholds: {
        business_success_rate: ['rate>0.9'],
    },
};

export default function (data) {
    const token = data.buyerTokens[__VU % data.buyerTokens.length];
    const auctionId = data.auctionIds[0];

    const currentRes = http.get(
        `${BASE_URL}/api/auctions/${auctionId}/bids/current/v1`,
        {
            ...authHeaders(token),
            responseCallback: http.expectedStatuses(200, 404),
            timeout: '60s',
        }
    );

    let price;
    if (currentRes.status === 200) {
        price = currentRes.json('data.price') - 1;  // 랜덤 없이 딱 1만 빼서 동시에 같은 가격
    } else {
        price = 99999999;
    }

    // sleep 없음! 최대한 동시에 몰리게

    const params = {
        ...authHeaders(token),
        responseCallback: http.expectedStatuses(201, 400, 409),
        timeout: '60s',
    };

    const res = http.post(
        `${BASE_URL}/api/auctions/${auctionId}/bids/v2`,
        JSON.stringify({ price }),
        params
    );

    const success = res.status === 201 || res.status === 400 || res.status === 409;
    businessSuccessRate.add(success);
    if (success) successCount.add(1);
    else failCount.add(1);

    check(res, {
        'success': (r) => r.status === 201 || r.status === 400 || r.status === 409,
        'no server error': (r) => r.status < 500,
    });

    sleep(0.1);
}