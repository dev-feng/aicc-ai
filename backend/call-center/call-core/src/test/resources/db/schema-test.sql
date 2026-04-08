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

CREATE TABLE IF NOT EXISTS call_recording (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  call_id VARCHAR(64) NOT NULL,
  recording_path VARCHAR(255) DEFAULT NULL,
  resource_id VARCHAR(128) DEFAULT NULL,
  format VARCHAR(32) DEFAULT NULL,
  duration_sec INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'pending',
  source_type VARCHAR(16) NOT NULL DEFAULT 'stub',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_call_recording_call_id ON call_recording(call_id);
CREATE INDEX IF NOT EXISTS idx_call_recording_status ON call_recording(status);

CREATE TABLE IF NOT EXISTS call_summary (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  call_id VARCHAR(64) NOT NULL,
  summary_text CLOB,
  summary_status VARCHAR(16) NOT NULL DEFAULT 'pending',
  result_code VARCHAR(64) DEFAULT NULL,
  tags VARCHAR(255) DEFAULT NULL,
  source_type VARCHAR(16) NOT NULL DEFAULT 'stub',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_call_summary_call_id ON call_summary(call_id);
CREATE INDEX IF NOT EXISTS idx_call_summary_status ON call_summary(summary_status);

CREATE TABLE IF NOT EXISTS transfer_result (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  call_id VARCHAR(64) NOT NULL,
  attempt_no INT NOT NULL DEFAULT 1,
  target_agent_id BIGINT DEFAULT NULL,
  target_extension_no VARCHAR(32) DEFAULT NULL,
  transfer_status VARCHAR(16) NOT NULL DEFAULT 'pending',
  failure_reason VARCHAR(64) DEFAULT NULL,
  fallback_action VARCHAR(64) DEFAULT NULL,
  requested_at DATETIME DEFAULT NULL,
  completed_at DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_transfer_result_call_attempt ON transfer_result(call_id, attempt_no);
CREATE INDEX IF NOT EXISTS idx_transfer_result_status ON transfer_result(transfer_status);
