import http from 'k6/http';
import { check, sleep } from 'k6';

const TARGET = __ENV.TARGET || 'http://localhost:8080';
const MANAGERS = [];
for (let i = 0; i < 30; i++) MANAGERS.push(`mgr${String(i).padStart(2, '0')}`);

export const options = { vus: 100, duration: '90s' };

export function setup() {
  const tokens = [];
  for (const user of MANAGERS) {
    const res = http.post(`${TARGET}/api/auth/login`,
      JSON.stringify({ username: user, password: 'Admin@123' }),
      { headers: { 'Content-Type': 'application/json' } });
    check(res, { 'login ok': (r) => r.json('code') === 200 });
    tokens.push(res.json('data.token'));
  }
  return { tokens };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];

  const res = http.get(
    `${TARGET}/api/user/tasks/pending?pageNum=1&pageSize=10`,
    { headers: { 'Authorization': token } }
  );
  check(res, {
    'status 200': (r) => r.status === 200,
    'pending ok': (r) => r.json('code') === 200,
  });

  sleep(1);
}
