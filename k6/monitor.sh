#!/bin/bash
PROM="http://localhost:9090/api/v1/query"
INTERVAL=10
TIMES=30
OUTPUT="monitor_$(date +%Y%m%d_%H%M%S).txt"

query() {
  local promql="$1"
  curl -sG "$PROM" --data-urlencode "query=$promql" | python3 -c "
import sys, json
d = json.load(sys.stdin)
if d['data']['result']:
    print(d['data']['result'][0]['value'][1], end='')
else:
    print('no data', end='')
"
}

HEADER="%-10s %-8s %-8s %-8s %-8s %-8s %-8s %-8s %-8s %-8s %-10s\n"
printf "$HEADER" "TIME" "QPS" "P95" "P99" "ERR%" "TC_BUSY" "TC_MAX" "HK_ACT" "HK_PEND" "HK_MAX" "JVM_MB" | tee "$OUTPUT"

for i in $(seq 1 $TIMES); do
  TIME=$(date +%H:%M:%S)
  QPS=$(query 'sum(rate(http_server_requests_seconds_count{application="approval-flow-backend", uri!="/actuator/prometheus"}[1m]))')
  P95=$(query 'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{application="approval-flow-backend", uri!="/actuator/prometheus"}[1m])))')
  P99=$(query 'histogram_quantile(0.99, sum by (le) (rate(http_server_requests_seconds_bucket{application="approval-flow-backend", uri!="/actuator/prometheus"}[1m])))')
  ERR=$(query 'sum(rate(http_server_requests_seconds_count{application="approval-flow-backend", status=~"5.."}[1m])) / sum(rate(http_server_requests_seconds_count{application="approval-flow-backend"}[1m])) * 100')
  TC_BUSY=$(query 'sum(tomcat_threads_busy_threads{application="approval-flow-backend"})')
  TC_MAX=$(query 'sum(tomcat_threads_config_max_threads{application="approval-flow-backend"})')
  HK_ACT=$(query 'sum(hikaricp_connections_active{application="approval-flow-backend"})')
  HK_PEND=$(query 'sum(hikaricp_connections_pending{application="approval-flow-backend"})')
  HK_MAX=$(query 'sum(hikaricp_connections_max{application="approval-flow-backend"})')
  JVM=$(query 'sum(jvm_memory_used_bytes{application="approval-flow-backend", area="heap"}) / 1048576')

  printf "$HEADER" "$TIME" "$QPS" "$P95" "$P99" "$ERR" "$TC_BUSY" "$TC_MAX" "$HK_ACT" "$HK_PEND" "$HK_MAX" "$JVM" | tee -a "$OUTPUT"
  sleep $INTERVAL
done

echo ""
echo "结果已保存到 $OUTPUT"
