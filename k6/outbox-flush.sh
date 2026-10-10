#!/bin/bash
TARGET="${TARGET:-http://localhost:8080}"   # 后端不在本机时用环境变量 TARGET 覆盖

TOKEN=$(curl -s -X POST "$TARGET/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}' | jq -r '.data.token')
[ -z "$TOKEN" -o "$TOKEN" = "null" ] && { echo "login FAILED"; exit 1; }

docker exec -i approval-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 approval_flow < "$(dirname "$0")/outbox-seed.sql"

INITIAL=$(curl -s "$TARGET/api/admin/mq-outbox/overview" -H "Authorization: $TOKEN" | jq -r '.data.pending')
echo "seeded, initial pending=$INITIAL at $(date +%T)"

START=$(date +%s)
while true; do
  RESP=$(curl -s "$TARGET/api/admin/mq-outbox/overview" -H "Authorization: $TOKEN")
  PENDING=$(echo "$RESP" | jq -r '.data.pending')
  DEAD=$(echo "$RESP" | jq -r '.data.dead')

  if ! [[ "$PENDING" =~ ^[0-9]+$ && "$DEAD" =~ ^[0-9]+$ ]]; then
    echo "[warn] bad response: $RESP"
    sleep 5
    continue
  fi

  ELAPSED=$(($(date +%s) - START))
  if [ "$ELAPSED" -gt 0 ]; then
    RATE=$(echo "scale=2; ($INITIAL - $PENDING) / $ELAPSED" | bc)
  else
    RATE=0
  fi
  echo "[${ELAPSED}s] pending=$PENDING dead=$DEAD rate=${RATE}/s"

  if [ "$PENDING" = "0" ]; then
    echo "DONE in ${ELAPSED}s (avg ${RATE}/s)"
    break
  fi
  if [ "$DEAD" -gt 100 ]; then
    echo "TOO MANY DEAD: $DEAD, stopping"
    break
  fi
  sleep 5
done
