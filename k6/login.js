import http from 'k6/http';
import { check, sleep } from 'k6';

const TARGET = __ENV.TARGET || 'http://localhost:8080';

export const options = {
  vus:100,
  duration: '90s',
};

const USERS = [];
for (let i = 0; i < 40; i++) USERS.push(`staff${String(i).padStart(2, '0')}`);
for (let i = 0; i < 30; i++) USERS.push(`mgr${String(i).padStart(2, '0')}`);
for (let i = 0; i < 20; i++) USERS.push(`dir${String(i).padStart(2, '0')}`);
for (let i = 0; i < 5; i++) USERS.push(`pres${String(i).padStart(2, '0')}`);

export default function () {
  const user = USERS[Math.floor(Math.random() * USERS.length)];
  const res = http.post(
    `${TARGET}/api/auth/login`,
    JSON.stringify({ username: user, password: 'Admin@123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const body = res.json();
  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'body code 200': () => body.code === 200,
  });

  if (!ok) {
    console.log(`FAIL user=${user} status=${res.status} code=${body.code} msg=${body.message || body.msg || JSON.stringify(body)}`);
  }

  sleep(1);
}
