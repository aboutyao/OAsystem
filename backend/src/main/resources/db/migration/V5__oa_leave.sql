CREATE TABLE oa_leave (
    id BIGINT NOT NULL,
    process_instance_id VARCHAR(128),
    wf_instance_id BIGINT,
    rule_version_id BIGINT,
    leave_type VARCHAR(64) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    duration_hours DECIMAL(10, 2) NOT NULL,
    duration_days DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(1000),
    handover_note VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_name_snapshot VARCHAR(64) NOT NULL,
    created_dept_id BIGINT,
    created_dept_name_snapshot VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_oa_leave_created_by ON oa_leave (created_by);
CREATE INDEX idx_oa_leave_dept ON oa_leave (created_dept_id);
CREATE INDEX idx_oa_leave_status ON oa_leave (status);
CREATE INDEX idx_oa_leave_start_end ON oa_leave (start_at, end_at);
CREATE INDEX idx_oa_leave_process ON oa_leave (process_instance_id);
CREATE INDEX idx_oa_leave_wf_inst ON oa_leave (wf_instance_id);
