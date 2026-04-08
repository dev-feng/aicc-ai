-- 第三阶段最小数据模型：录音、摘要、转人工结果

CREATE TABLE IF NOT EXISTS `call_recording` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `call_id` VARCHAR(64) NOT NULL COMMENT '呼叫唯一标识',
  `recording_path` VARCHAR(255) DEFAULT NULL COMMENT '录音文件路径',
  `resource_id` VARCHAR(128) DEFAULT NULL COMMENT '外部资源标识',
  `format` VARCHAR(32) DEFAULT NULL COMMENT '录音格式，如 wav/mp3',
  `duration_sec` INT NOT NULL DEFAULT 0 COMMENT '录音时长（秒）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/success/failed/cancelled',
  `source_type` VARCHAR(16) NOT NULL DEFAULT 'stub' COMMENT '来源: real/stub/mock',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_recording_call_id` (`call_id`),
  KEY `idx_call_recording_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话录音元数据表';

CREATE TABLE IF NOT EXISTS `call_summary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `call_id` VARCHAR(64) NOT NULL COMMENT '呼叫唯一标识',
  `summary_text` TEXT COMMENT '摘要正文',
  `summary_status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/success/failed/cancelled',
  `result_code` VARCHAR(64) DEFAULT NULL COMMENT '处理结果编码',
  `tags` VARCHAR(255) DEFAULT NULL COMMENT '关键标签，逗号分隔',
  `source_type` VARCHAR(16) NOT NULL DEFAULT 'stub' COMMENT '来源: real/stub/mock',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_summary_call_id` (`call_id`),
  KEY `idx_call_summary_status` (`summary_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话摘要表';

CREATE TABLE IF NOT EXISTS `transfer_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `call_id` VARCHAR(64) NOT NULL COMMENT '呼叫唯一标识',
  `attempt_no` INT NOT NULL DEFAULT 1 COMMENT '转接尝试次数，从1开始',
  `target_agent_id` BIGINT DEFAULT NULL COMMENT '目标坐席ID',
  `target_extension_no` VARCHAR(32) DEFAULT NULL COMMENT '目标分机号',
  `transfer_status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/success/failed/timeout/cancelled',
  `failure_reason` VARCHAR(64) DEFAULT NULL COMMENT '失败原因',
  `fallback_action` VARCHAR(64) DEFAULT NULL COMMENT '兜底动作',
  `requested_at` DATETIME DEFAULT NULL COMMENT '请求转接时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '转接完成时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transfer_result_call_attempt` (`call_id`, `attempt_no`),
  KEY `idx_transfer_result_status` (`transfer_status`),
  KEY `idx_transfer_result_agent` (`target_agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='转人工结果表';
