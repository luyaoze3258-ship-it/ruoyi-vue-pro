CREATE TABLE IF NOT EXISTS `bpm_ai_approval_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `process_instance_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程实例编号',
  `process_definition_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程定义编号',
  `task_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程任务编号',
  `task_definition_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '流程任务定义 Key',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '流程任务名称',
  `assignee_user_id` bigint NULL DEFAULT NULL COMMENT '任务创建时审批人编号',
  `external_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '外部业务编号',
  `guanlan_task_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '观澜任务编号',
  `guanlan_agent_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '观澜智能体名称',
  `guanlan_base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '观澜 API Base URL',
  `guanlan_api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '观澜 Agent API Key',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用 AI 审批',
  `adopt_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否采纳 AI 结论',
  `status` tinyint NOT NULL COMMENT '状态',
  `verdict` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'AI 结论',
  `opinion` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'AI 意见',
  `callback_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '回调编号',
  `callback_time` datetime NULL DEFAULT NULL COMMENT '回调时间',
  `business_final_verdict` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务最终结论',
  `business_final_opinion` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务最终意见',
  `business_result_sync_time` datetime NULL DEFAULT NULL COMMENT '业务结果回灌时间',
  `submit_error` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '提交错误',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_bpm_ai_task_tenant_task` (`tenant_id`, `task_id`) USING BTREE,
  UNIQUE KEY `uk_bpm_ai_task_tenant_external` (`tenant_id`, `external_id`) USING BTREE,
  KEY `idx_bpm_ai_task_tenant_guanlan` (`tenant_id`, `guanlan_task_id`) USING BTREE,
  KEY `idx_bpm_ai_task_tenant_instance` (`tenant_id`, `process_instance_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'BPM AI 审批任务';

SET @column_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bpm_ai_approval_task' AND COLUMN_NAME = 'guanlan_agent_name'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE `bpm_ai_approval_task` ADD COLUMN `guanlan_agent_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT ''观澜智能体名称'' AFTER `guanlan_task_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bpm_ai_approval_task' AND COLUMN_NAME = 'guanlan_base_url'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE `bpm_ai_approval_task` ADD COLUMN `guanlan_base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT ''观澜 API Base URL'' AFTER `guanlan_agent_name`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bpm_ai_approval_task' AND COLUMN_NAME = 'guanlan_api_key'
);
SET @ddl := IF(@column_exists = 0,
  'ALTER TABLE `bpm_ai_approval_task` ADD COLUMN `guanlan_api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT ''观澜 Agent API Key'' AFTER `guanlan_base_url`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `bpm_ai_approval_callback_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `callback_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '回调编号',
  `guanlan_task_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '观澜任务编号',
  `external_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '外部业务编号',
  `verdict` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'AI 结论',
  `test_mode` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否测试模式',
  `verified` bit(1) NOT NULL DEFAULT b'0' COMMENT '签名是否通过',
  `processed` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否处理完成',
  `raw_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原始回调请求体',
  `error_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误信息',
  `received_time` datetime NOT NULL COMMENT '接收时间',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_bpm_ai_callback_tenant_callback` (`tenant_id`, `callback_id`) USING BTREE,
  KEY `idx_bpm_ai_callback_tenant_guanlan` (`tenant_id`, `guanlan_task_id`) USING BTREE,
  KEY `idx_bpm_ai_callback_tenant_external` (`tenant_id`, `external_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'BPM AI 审批回调日志';
