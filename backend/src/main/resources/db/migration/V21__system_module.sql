CREATE TABLE sys_number_rule (
    id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    prefix VARCHAR(32) NOT NULL,
    date_pattern VARCHAR(32),
    seq_length INT NOT NULL,
    seq_reset VARCHAR(16) NOT NULL,
    current_period VARCHAR(32),
    current_seq BIGINT NOT NULL DEFAULT 0,
    description VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_number_rule_code UNIQUE (rule_code)
);

CREATE INDEX idx_sys_number_rule_business ON sys_number_rule (business_type);

CREATE TABLE sys_work_calendar (
    id BIGINT NOT NULL,
    cal_date DATE NOT NULL,
    day_type VARCHAR(16) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_work_calendar_date UNIQUE (cal_date)
);

CREATE INDEX idx_sys_work_calendar_type ON sys_work_calendar (day_type);

CREATE TABLE sys_import_task (
    id BIGINT NOT NULL,
    task_code VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    failed_rows INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    error_summary VARCHAR(2000),
    submitted_by BIGINT NOT NULL,
    submitted_at DATETIME NOT NULL,
    finished_at DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_import_task_code UNIQUE (task_code)
);

CREATE INDEX idx_sys_import_task_business ON sys_import_task (business_type);
CREATE INDEX idx_sys_import_task_submitted_by ON sys_import_task (submitted_by);

CREATE TABLE sys_export_task (
    id BIGINT NOT NULL,
    task_code VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    filter_json TEXT,
    file_name VARCHAR(255),
    file_size BIGINT,
    row_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    error_summary VARCHAR(2000),
    submitted_by BIGINT NOT NULL,
    submitted_at DATETIME NOT NULL,
    finished_at DATETIME,
    download_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_export_task_code UNIQUE (task_code)
);

CREATE INDEX idx_sys_export_task_business ON sys_export_task (business_type);
CREATE INDEX idx_sys_export_task_submitted_by ON sys_export_task (submitted_by);

INSERT INTO sys_number_rule (
    id, rule_code, business_type, prefix, date_pattern, seq_length, seq_reset,
    current_period, current_seq, description, status, created_at, updated_at, version
) VALUES
    (1, 'EXPENSE_NO', 'EXPENSE', 'EXP', 'yyyyMMdd', 4, 'DAILY', NULL, 0, '报销单号 EXP+日期+4位流水', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2, 'LEAVE_NO', 'LEAVE', 'LV', 'yyyyMMdd', 4, 'DAILY', NULL, 0, '请假单号 LV+日期+4位流水', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3, 'PURCHASE_NO', 'PURCHASE', 'PUR', 'yyyyMM', 5, 'MONTHLY', NULL, 0, '采购单号 PUR+年月+5位流水', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4, 'CONTRACT_NO', 'CONTRACT', 'CT', 'yyyy', 5, 'YEARLY', NULL, 0, '合同编号 CT+年份+5位流水', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
