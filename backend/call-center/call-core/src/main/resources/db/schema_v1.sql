-- 通话记录表（幂等建表，可重复执行）
-- 对应 SYSTEM_SPEC.md 6.2 核心表

CREATE TABLE IF NOT EXISTS `call_record` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `call_id`          VARCHAR(32)  NOT NULL COMMENT '呼叫唯一标识（UUID）',
  `caller`           VARCHAR(20)  NOT NULL COMMENT '主叫号码',
  `callee`           VARCHAR(20)  NOT NULL COMMENT '被叫号码',
  `call_type`        TINYINT      NOT NULL COMMENT '呼叫类型：1-呼入，2-呼出',
  `start_time`       DATETIME     NOT NULL COMMENT '呼叫开始时间',
  `ringing_time`     DATETIME     DEFAULT NULL COMMENT '振铃开始时间',
  `answer_time`      DATETIME     DEFAULT NULL COMMENT '接通时间',
  `end_time`         DATETIME     DEFAULT NULL COMMENT '呼叫结束时间',
  `status`           VARCHAR(10)  NOT NULL COMMENT '状态：ringing/answered/hungup',
  `ringing_duration` INT          DEFAULT 0 COMMENT '振铃时长（秒）',
  `answer_duration`  INT          DEFAULT 0 COMMENT '接通时长（秒）',
  `call_duration`    INT          DEFAULT 0 COMMENT '通话总时长（秒）',
  `hangup_cause`     VARCHAR(64)  DEFAULT NULL COMMENT '挂断原因（ESL hangup cause）',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_call_id` (`call_id`),
  KEY `idx_caller` (`caller`),
  KEY `idx_callee` (`callee`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通话记录表';
