#!/bin/bash
HASH='$2a$10$P5VUbno17Xwv54nSrs7S5Oaa1Nh1PR4E4uiEzo2RWyw2ObbeqM8c2'
DB="docker exec -i approval-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 approval_flow"

$DB -e "
INSERT INTO user (id, username, password, name, email, role_id, manager_id, status)
SELECT 1301+n, CONCAT('pres', LPAD(n,2,'0')), '$HASH', CONCAT('压测总监',n),
       CONCAT('pres',n,'@test.com'), 5, NULL, 1
FROM (SELECT a.n AS n
      FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) a) t
WHERE n < 5;

INSERT INTO user (id, username, password, name, email, role_id, manager_id, status)
SELECT 1201+n, CONCAT('dir', LPAD(n,2,'0')), '$HASH', CONCAT('压测经理',n),
       CONCAT('dir',n,'@test.com'), 4, 1301+(n MOD 5), 1
FROM (SELECT a.n + b.n * 10 AS n
      FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
            UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
           (SELECT 0 AS n UNION SELECT 1) b) t
WHERE n < 20;

INSERT INTO user (id, username, password, name, email, role_id, manager_id, status)
SELECT 1101+n, CONCAT('mgr', LPAD(n,2,'0')), '$HASH', CONCAT('压测主管',n),
       CONCAT('mgr',n,'@test.com'), 3, 1201+(n MOD 20), 1
FROM (SELECT a.n + b.n * 10 AS n
      FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
            UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
           (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2) b) t
WHERE n < 30;

INSERT INTO user (id, username, password, name, email, role_id, manager_id, status)
SELECT 1001+n, CONCAT('staff', LPAD(n,2,'0')), '$HASH', CONCAT('压测员工',n),
       CONCAT('staff',n,'@test.com'), 2, 1101+(n MOD 30), 1
FROM (SELECT a.n + b.n * 10 AS n
      FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
            UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
           (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) b) t
WHERE n < 40;
"
