CREATE DATABASE IF NOT EXISTS approval_flow
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

use approval_flow;

-- ======================================================
-- 1. 角色表（role）
-- ======================================================
CREATE TABLE `role` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`        VARCHAR(50) NOT NULL COMMENT '角色编码，如：ADMIN、STAFF、MANAGER、DIRECTOR、PRESIDENT',
    `name`        VARCHAR(50) NOT NULL COMMENT '角色名称，如：管理员、员工、主管、经理、总监',
    `level`       INT         NOT NULL COMMENT '角色等级，数字越小等级越低',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ======================================================
-- 2. 用户表（user）
-- ======================================================
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号，唯一',
    `password`    VARCHAR(100) NOT NULL COMMENT '加密密码',
    `name`        VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `email`       VARCHAR(100) NOT NULL COMMENT '邮箱',
    `role_id`     BIGINT       DEFAULT NULL COMMENT '关联 role 表主键',
    `manager_id`  BIGINT       DEFAULT NULL COMMENT '直属上级ID',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_manager_id` (`manager_id`),
    CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_user_manager` FOREIGN KEY (`manager_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ======================================================
-- 3. 流程定义表（process_definition）
-- ======================================================
CREATE TABLE `process_definition` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100) NOT NULL COMMENT '流程名称，唯一',
    `version`     INT          NOT NULL DEFAULT 1 COMMENT '版本号，每次编辑时加1（仅用于记录修改次数，不做版本隔离）',
    `role_code`   VARCHAR(50)  DEFAULT NULL COMMENT '适用角色编码，NULL表示所有角色可用',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    `form_schema` JSON         DEFAULT NULL COMMENT '表单结构定义，存储该流程的字段配置（字段名、标签、类型、是否必填等）',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- ======================================================
-- 4. 流程节点表（process_node）
-- ======================================================
CREATE TABLE `process_node` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `definition_id`  BIGINT       NOT NULL COMMENT '关联 process_definition 表主键',
    `node_name`      VARCHAR(100) NOT NULL COMMENT '节点名称（如：主管审批、经理审批）',
    `node_order`     INT          NOT NULL COMMENT '节点顺序：1,2,3...',
    `assignee_type`  VARCHAR(20)  NOT NULL COMMENT '分配策略：RELATIVE(相对关系), USER(指定人), ROLE(角色)',
    `assignee_expr`  VARCHAR(255) NOT NULL COMMENT '分配目标值：关系字段名(如manager_id)、用户ID、角色编码',
    `condition_expr` TEXT         DEFAULT NULL COMMENT '生成该节点任务的条件表达式（Aviator语法），为空表示无条件生成',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_definition_id` (`definition_id`),
    KEY `idx_definition_order` (`definition_id`, `node_order`),
    CONSTRAINT `fk_node_definition` FOREIGN KEY (`definition_id`) REFERENCES `process_definition` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点表';

-- ======================================================
-- 5. 流程实例表（process_instance）
-- ======================================================
CREATE TABLE `process_instance` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `definition_id`   BIGINT       NOT NULL COMMENT '关联 process_definition 表主键',
    `definition_name` VARCHAR(100) NOT NULL COMMENT '流程名称快照，创建实例时存入',
    `initiator_id`    BIGINT       NOT NULL COMMENT '发起人ID，关联 user 表主键',
    `form_data`       JSON         DEFAULT NULL COMMENT '存业务数据快照',
    `submit_count`    INT          NOT NULL DEFAULT 1 COMMENT '提交次数，默认 1，每次修改重提 +1',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1审批中 2通过 3驳回 4撤回',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_definition_id` (`definition_id`),
    KEY `idx_initiator_id` (`initiator_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程实例表';

-- ======================================================
-- 6. 任务表（task）
-- ======================================================
CREATE TABLE `task` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `instance_id`  BIGINT       NOT NULL COMMENT '关联 process_instance 表主键',
    `node_id`      BIGINT       DEFAULT NULL COMMENT '关联 process_node 表主键',
    `node_name`    VARCHAR(100) NOT NULL COMMENT '节点名称快照，创建任务时存入',
    `assignee_id`  BIGINT       NOT NULL COMMENT '审批人ID，关联 user 表主键',
    `submit_round` INT          NOT NULL COMMENT '提交轮次，标识该任务是第几轮提交生成的',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2已取消',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_assignee_id_status` (`assignee_id`, `status`),
    KEY `idx_instance_submit` (`instance_id`, `submit_round`),
    CONSTRAINT `fk_task_instance` FOREIGN KEY (`instance_id`) REFERENCES `process_instance` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_task_node` FOREIGN KEY (`node_id`) REFERENCES `process_node` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_task_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- ======================================================
-- 7. 审批记录表（approval_record）- 引用 process_instance、process_node、user、task
-- ======================================================
CREATE TABLE `approval_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `instance_id` BIGINT       NOT NULL COMMENT '关联 process_instance 表主键',
    `operator_id` BIGINT       NOT NULL COMMENT '操作人ID，关联 user 表主键',
    `task_id`     BIGINT       DEFAULT NULL COMMENT '关联 task.id，允许 NULL',
    `action`      TINYINT      NOT NULL COMMENT '0提交 1通过 2驳回 3撤回 4修改重提',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '审批意见',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance_id` (`instance_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_instance_time` (`instance_id`, `create_time`),
    CONSTRAINT `fk_record_instance` FOREIGN KEY (`instance_id`) REFERENCES `process_instance` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_record_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- ======================================================
-- 初始化角色数据
-- ======================================================
INSERT INTO `role` (`id`, `code`, `name`, `level`, `status`, `create_time`, `update_time`) VALUES
(1, 'ADMIN', '管理员', 0, 1, NOW(), NOW()),
(2, 'STAFF', '员工', 1, 1, NOW(), NOW()),
(3, 'MANAGER', '主管', 2, 1, NOW(), NOW()),
(4, 'DIRECTOR', '经理', 3, 1, NOW(), NOW()),
(5, 'PRESIDENT', '总监', 4, 1, NOW(), NOW());

-- ======================================================
-- 初始化用户数据
-- ======================================================
-- 密码统一为：Admin@123（BCrypt加密后的结果）
INSERT INTO `user` (`id`, `username`, `password`, `name`, `email`, `role_id`, `manager_id`, `status`) VALUES
(1, 'admin', '$2a$10$P5VUbno17Xwv54nSrs7S5Oaa1Nh1PR4E4uiEzo2RWyw2ObbeqM8c2', '管理员', 'admin@example.com', 1, NULL, 1),
(2, 'zhaoliu', '$2a$10$P5VUbno17Xwv54nSrs7S5Oaa1Nh1PR4E4uiEzo2RWyw2ObbeqM8c2', '赵六', 'zhaoliu@example.com', 5, NULL, 1),
(3, 'lisi', '$2a$10$P5VUbno17Xwv54nSrs7S5Oaa1Nh1PR4E4uiEzo2RWyw2ObbeqM8c2', '李四', 'lisi@example.com', 4, 2, 1),
(4, 'wangwu', '$2a$10$P5VUbno17Xwv54nSrs7S5Oaa1Nh1PR4E4uiEzo2RWyw2ObbeqM8c2', '王五', 'wangwu@example.com', 3, 3, 1),
(5, 'zhangsan', '$2a$10$P5VUbno17Xwv54nSrs7S5Oaa1Nh1PR4E4uiEzo2RWyw2ObbeqM8c2', '张三', 'zhangsan@example.com', 2, 4, 1);

-- ======================================================
-- 初始化流程定义数据
-- ======================================================
INSERT INTO `process_definition` (`id`, `name`, `version`, `role_code`, `status`, `form_schema`, `create_time`, `update_time`) VALUES
(1, '员工请假流程', 1, 'STAFF', 1, '[{"field": "days", "label": "请假天数", "type": "number", "required": true}, {"field": "reason", "label": "请假原因", "type": "textarea", "required": true}]', NOW(), NOW()),
(2, '员工报销流程', 1, 'STAFF', 1, '[{"field": "amount", "label": "报销金额", "type": "number", "required": true}, {"field": "description", "label": "费用说明", "type": "textarea", "required": true}]', NOW(), NOW());

-- ======================================================
-- 初始化请假流程节点数据
-- ======================================================
INSERT INTO `process_node` (`id`, `definition_id`, `node_name`, `node_order`, `assignee_type`, `assignee_expr`, `condition_expr`, `create_time`, `update_time`) VALUES
(1, 1, '主管审批', 1, 'RELATIVE', 'manager_id', 'days <= 3', NOW(), NOW()),
(2, 1, '经理审批', 2, 'RELATIVE', 'manager_id.manager_id', 'days > 3', NOW(), NOW());

-- ======================================================
-- 初始化报销流程节点数据
-- ======================================================
INSERT INTO `process_node` (`id`, `definition_id`, `node_name`, `node_order`, `assignee_type`, `assignee_expr`, `condition_expr`, `create_time`, `update_time`) VALUES
(3, 2, '主管审批', 1, 'RELATIVE', 'manager_id', 'amount <= 1000', NOW(), NOW()),
(4, 2, '经理审批', 2, 'RELATIVE', 'manager_id.manager_id', 'amount > 1000', NOW(), NOW());