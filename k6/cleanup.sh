#!/bin/bash
docker exec -i approval-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 -N approval_flow -e "
DELETE FROM mq_outbox;
DELETE FROM notification;
DELETE FROM approval_record;
DELETE FROM task;
DELETE FROM process_instance;
SELECT 'process_instance:', COUNT(*) FROM process_instance;
SELECT 'task:', COUNT(*) FROM task;
SELECT 'mq_outbox:', COUNT(*) FROM mq_outbox;"
