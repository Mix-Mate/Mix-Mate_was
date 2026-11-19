import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const GROUP_ID = __ENV.GROUP_ID || '1';
const SSE_HOLDERS = parseInt(__ENV.SSE_HOLDERS || '0', 10);
const API_RATE = parseInt(__ENV.API_RATE || '20', 10);
const API_DURATION = __ENV.API_DURATION || '30s';
const API_TIMEOUT = __ENV.API_TIMEOUT || '10s';

// 타임라인: 0~6s 워밍업 → 6s SSE 구독자 연결 → 12s API 측정 시작 → 42s 종료
const scenarios = {
  warmup: {
    executor: 'constant-arrival-rate',
    rate: 5, timeUnit: '1s', duration: '6s',
    preAllocatedVUs: 5, maxVUs: 20,
    exec: 'warmup',
  },
  api: {
    executor: 'constant-arrival-rate',
    rate: API_RATE, timeUnit: '1s', duration: API_DURATION,
    preAllocatedVUs: 50, maxVUs: 500,
    startTime: '12s',
    exec: 'api',
  },
};

if (SSE_HOLDERS > 0) {
  scenarios.sse = {
    executor: 'per-vu-iterations',
    vus: SSE_HOLDERS, iterations: 1, maxDuration: '60s',
    startTime: '6s',
    exec: 'sse',
  };
}

export const options = {
  scenarios,
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  // 임계값은 판정이 아니라 type:api 서브메트릭을 요약에 내보내기 위한 용도다.
  thresholds: {
    'http_req_failed{type:api}': ['rate<0.01'],
    'http_req_duration{type:api}': ['p(95)<500'],
    'http_reqs{type:api}': ['count>=0'],
  },
};

export function setup() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email: __ENV.TEST_EMAIL, password: __ENV.TEST_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'login ok': (r) => r.status === 200 });
  return { token: res.json('accessToken') };
}

function listMyGroups(data, tags) {
  return http.get(`${BASE_URL}/api/v1/groups?scope=me&state=active`, {
    headers: { Authorization: `Bearer ${data.token}` },
    timeout: API_TIMEOUT,
    tags,
  });
}

export function warmup(data) {
  listMyGroups(data, { type: 'warmup' });
}

export function api(data) {
  const res = listMyGroups(data, { type: 'api' });
  check(res, { 'api 200': (r) => r.status === 200 }, { type: 'api' });
}

// SSE 구독자: 연결을 붙여 둔 채로 측정 구간이 끝날 때까지 유지한다. (타임아웃으로 끊기는 것이 정상 종료)
export function sse(data) {
  http.get(`${BASE_URL}/api/v1/groups/${GROUP_ID}/status/stream`, {
    headers: { Authorization: `Bearer ${data.token}`, Accept: 'text/event-stream' },
    timeout: '38s',
    tags: { type: 'sse' },
  });
}
