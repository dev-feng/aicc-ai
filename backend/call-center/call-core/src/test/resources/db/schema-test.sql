CREATE TABLE IF NOT EXISTS call_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  call_id VARCHAR(64) NOT NULL,
  caller VARCHAR(20) NOT NULL,
  callee VARCHAR(20) NOT NULL,
  call_type TINYINT NOT NULL,
  start_time DATETIME NOT NULL,
  ringing_time DATETIME DEFAULT NULL,
  answer_time DATETIME DEFAULT NULL,
  end_time DATETIME DEFAULT NULL,
  status VARCHAR(10) NOT NULL,
  ringing_duration INT DEFAULT 0,
  answer_duration INT DEFAULT 0,
  call_duration INT DEFAULT 0,
  hangup_cause VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_call_id ON call_record(call_id);
CREATE INDEX IF NOT EXISTS idx_caller ON call_record(caller);
CREATE INDEX IF NOT EXISTS idx_callee ON call_record(callee);
CREATE INDEX IF NOT EXISTS idx_start_time ON call_record(start_time);

CREATE TABLE IF NOT EXISTS agent_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_code VARCHAR(64) NOT NULL,
  agent_name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'offline',
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_code ON agent_info(agent_code);

CREATE TABLE IF NOT EXISTS agent_extension_binding (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_id BIGINT NOT NULL,
  extension_no VARCHAR(32) NOT NULL,
  binding_status VARCHAR(16) NOT NULL DEFAULT 'active',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_extension_no ON agent_extension_binding(extension_no);
CREATE INDEX IF NOT EXISTS idx_agent_id ON agent_extension_binding(agent_id);
