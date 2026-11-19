import http from 'k6/http';
import { check } from 'k6';

// 일반 용량 테스트: SCENARIO = smoke | baseline | load | spike | stress
// arrival-rate(초당 요청 발생률)로 부하를 정의한다. TPS = 초당 API 요청 수(req/s).
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SCENARIO = __ENV.SCENARIO || 'baseline';
const GROUP_IDS = (__ENV.GROUP_IDS || '1').split(',');

function stressStages() {
  const stages = [];
  for (let rate = 50; rate <= 400; rate += 50) {          // 50 → 400 TPS, 30초 단위로 50씩 증가
    stages.push({ target: rate, duration: '5s' }, { target: rate, duration: '25s' });
  }
  return stages;
}

function breakpointStages() {
  const stages = [];
  for (let rate = 500; rate <= 3000; rate += 500) {       // 500 → 3000 TPS, 30초 단위로 500씩 증가 (포화 지점 탐색)
    stages.push({ target: rate, duration: '5s' }, { target: rate, duration: '25s' });
  }
  return stages;
}

const PROFILES = {
  smoke:    { startRate: 2,  stages: [{ target: 2, duration: '15s' }] },
  baseline: { startRate: 10, stages: [{ target: 10, duration: '60s' }] },
  load:     { startRate: 10, stages: [{ target: 100, duration: '60s' }, { target: 100, duration: '120s' }] },
  spike:    { startRate: 10, stages: [{ target: 150, duration: '10s' }, { target: 150, duration: '60s' }] },
  stress:   { startRate: 50, stages: stressStages() },
  breakpoint: { startRate: 500, stages: breakpointStages() },
};
const profile = PROFILES[SCENARIO];

export const options = {
  scenarios: {
    capacity: {
      executor: 'ramping-arrival-rate',
      startRate: profile.startRate,
      timeUnit: '1s',
      preAllocatedVUs: 200,
      maxVUs: 1500,
      stages: profile.stages,
    },
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  // 통과 기준: p95 200ms 미만, 오류율 1% 미만 (이 프로젝트에서 정한 기준이며 k6 측 값이라 네트워크 구간을 포함한다)
  thresholds: {
    http_req_duration: ['p(95)<200'],
    http_req_failed: ['rate<0.01'],
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

// 요청 비율: 내 그룹 목록 70%, 그룹 상세 30%  (읽기 API만 — 쓰기는 데이터가 쌓이므로 제외)
export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };
  let res;
  if (Math.random() < 0.7) {
    res = http.get(`${BASE_URL}/api/v1/groups?scope=me&state=active`, { headers, tags: { name: 'my-groups' } });
  } else {
    const id = GROUP_IDS[Math.floor(Math.random() * GROUP_IDS.length)];
    res = http.get(`${BASE_URL}/api/v1/groups/${id}`, { headers, tags: { name: 'group-detail' } });
  }
  check(res, { 'status 200': (r) => r.status === 200 });
}
