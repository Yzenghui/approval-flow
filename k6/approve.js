import http from 'k6/http';
import { check, sleep } from 'k6';

const TARGET = __ENV.TARGET || 'http://localhost:8080';
const MANAGERS = [];
for (let i = 0; i < 30; i++) MANAGERS.push(`mgr${String(i).padStart(2, '0')}`);

export const options = { vus: 100, duration: '90s' };

let failCount = 0;

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

  const pending = http.get(
    `${TARGET}/api/user/tasks/pending?pageNum=1&pageSize=50`,
    { headers: { 'Authorization': token } }
  );
  const tasks = pending.json('data.records');
  if (!tasks || tasks.length === 0) { sleep(1); return; }

  const taskId = tasks[Math.floor(Math.random() * tasks.length)].taskId;

  const res = http.post(
    `${TARGET}/api/user/tasks/${taskId}/approve`,
    JSON.stringify({ remark: 'k6 压测批准' }),
    { headers: { 'Content-Type': 'application/json', 'Authorization': token } }
  );
  check(res, { 'approve ok': (r) => r.status === 200 && r.json('code') === 200 });

  if (res.json('code') !== 200) {
    failCount++;
    if (failCount <= 10) {
      console.log(`approve FAIL: taskId=${taskId} code=${res.json('code')} msg=${res.json('message')}`);
    }
  }

  sleep(1);
}
