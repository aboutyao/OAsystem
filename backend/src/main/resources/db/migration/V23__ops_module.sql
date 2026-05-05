CREATE TABLE job_task_log (
    id BIGINT NOT NULL,
    job_code VARCHAR(64) NOT NULL,
    job_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME,
    duration_ms BIGINT,
    success_count BIGINT NOT NULL DEFAULT 0,
    fail_count BIGINT NOT NULL DEFAULT 0,
    fail_reason VARCHAR(2000),
    triggered_by VARCHAR(64),
    PRIMARY KEY (id)
);

CREATE INDEX idx_job_log_code ON job_task_log (job_code);
CREATE INDEX idx_job_log_status ON job_task_log (status);
CREATE INDEX idx_job_log_start ON job_task_log (start_at);

CREATE TABLE app_exception_log (
    id BIGINT NOT NULL,
    request_id VARCHAR(64),
    request_uri VARCHAR(500),
    request_method VARCHAR(16),
    user_id BIGINT,
    exception_class VARCHAR(255) NOT NULL,
    exception_message VARCHAR(2000),
    stack_trace VARCHAR(8000),
    severity VARCHAR(32) NOT NULL DEFAULT 'ERROR',
    occurred_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_app_exc_severity ON app_exception_log (severity);
CREATE INDEX idx_app_exc_time ON app_exception_log (occurred_at);
CREATE INDEX idx_app_exc_user ON app_exception_log (user_id);

CREATE TABLE backup_record (
    id BIGINT NOT NULL,
    backup_type VARCHAR(32) NOT NULL,
    backup_path VARCHAR(500),
    backup_size BIGINT,
    status VARCHAR(32) NOT NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME,
    duration_ms BIGINT,
    fail_reason VARCHAR(2000),
    triggered_by VARCHAR(64),
    PRIMARY KEY (id)
);

CREATE INDEX idx_backup_type ON backup_record (backup_type);
CREATE INDEX idx_backup_status ON backup_record (status);
CREATE INDEX idx_backup_started ON backup_record (started_at);

INSERT INTO job_task_log (id, job_code, job_name, status, start_at, end_at, duration_ms,
    success_count, fail_count, fail_reason, triggered_by)
VALUES
    (1, 'WF_REMINDER', '流程超时提醒任务', 'SUCCESS',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1320,
        12, 0, NULL, 'SYSTEM'),
    (2, 'NUMBER_RESET', '编号规则按周期重置', 'SUCCESS',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 250,
        4, 0, NULL, 'SYSTEM');

INSERT INTO backup_record (id, backup_type, backup_path, backup_size, status,
    started_at, finished_at, duration_ms, fail_reason, triggered_by)
VALUES
    (1, 'DATABASE', '/var/backups/oa/db_seed.sql.gz', 4096000, 'SUCCESS',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 18500,
        NULL, 'SYSTEM');
