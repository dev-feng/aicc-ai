CREATE TABLE IF NOT EXISTS `agent_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_code` VARCHAR(64) NOT NULL COMMENT '坐席编号',
  `agent_name` VARCHAR(64) NOT NULL COMMENT '坐席姓名',
  `status` VARCHAR(16) NOT NULL DEFAULT 'offline' COMMENT '坐席状态: offline/idle/busy/pause',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 1-启用, 0-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_code` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='坐席信息表';

CREATE TABLE IF NOT EXISTS `agent_extension_binding` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` BIGINT NOT NULL COMMENT '坐席ID',
  `extension_no` VARCHAR(32) NOT NULL COMMENT '分机号',
  `binding_status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '绑定状态: active/inactive',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_extension_no` (`extension_no`),
  KEY `idx_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='坐席分机绑定表';
