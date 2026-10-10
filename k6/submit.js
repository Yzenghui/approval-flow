import http from 'k6/http';
import { check, sleep } from 'k6';

const TARGET = __ENV.TARGET || 'http://localhost:8080';

export const options = { vus: 100, duration: '90s' };

const STAFF = [];
for (let i = 0; i < 40; i++) STAFF.push(`staff${String(i).padStart(2, '0')}`);

export function setup() {
  const tokens = [];
  for (const user of STAFF) {
    const res = http.post(`${TARGET}/api/auth/login`,
      JSON.stringify({ username: user, password: 'Admin@123' }),
      { headers: { 'Content-Type': 'application/json' } });
    check(res, { 'login ok': (r) => r.json('code') === 200 }) || console.log(`setup login FAIL: ${user}`);
    tokens.push(res.json('data.token'));
  }
  return { tokens };
}

export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const days = 1 + Math.floor(Math.random() * 3);
  const res = http.post(`${TARGET}/api/user/instances`,
    JSON.stringify({ definitionId: 1, formData: JSON.stringify({ days, reason: 'k6 压测提交' }) }),
    { headers: { 'Content-Type': 'application/json', 'Authorization': token } });
  check(res, {
    'status 200': (r) => r.status === 200,
    'submit ok': (r) => r.json('code') === 200,
  });
  sleep(1);
}
