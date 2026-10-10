SET SESSION cte_max_recursion_depth = 10001;

INSERT INTO mq_outbox (topic, msg_key, body, status, retry_count, next_retry_time, create_time, update_time)
WITH RECURSIVE nums AS (
  SELECT 1 AS n UNION ALL SELECT n + 1 FROM nums WHERE n < 10000
),
real_ids AS (
  SELECT t.id AS task_id, t.instance_id, n.recipient_id
  FROM notification n JOIN task t ON n.task_id = t.id
  WHERE n.type = 1 ORDER BY n.id DESC LIMIT 1
)
SELECT
  'approval-event',
  CONCAT('k6-seed-', n),
  CONCAT('{"instanceId":', r.instance_id, ',"taskId":', r.task_id, ',"type":1,"recipientId":', r.recipient_id, ',"content":"k6 seed"}'),
  0, 0, NOW(), NOW(), NOW()
FROM nums, real_ids r;
