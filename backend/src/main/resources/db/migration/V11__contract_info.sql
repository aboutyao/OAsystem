CREATE TABLE contract_info (
    id BIGINT NOT NULL,
    process_instance_id VARCHAR(128),
    wf_instance_id BIGINT,
    rule_version_id BIGINT,
    contract_no VARCHAR(64) NOT NULL,
    contract_name VARCHAR(255) NOT NULL,
    contract_type VARCHAR(64) NOT NULL,
    counterparty VARCHAR(255) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    start_date DATE,
    end_date DATE,
    sign_date DATE,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_name_snapshot VARCHAR(64) NOT NULL,
    created_dept_id BIGINT,
    created_dept_name_snapshot VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_contract_no UNIQUE (contract_no)
);

CREATE INDEX idx_contract_type ON contract_info (contract_type);
CREATE INDEX idx_contract_counterparty ON contract_info (counterparty);
CREATE INDEX idx_contract_end_date ON contract_info (end_date);
CREATE INDEX idx_contract_status ON contract_info (status);
CREATE INDEX idx_contract_created_by ON contract_info (created_by);
CREATE INDEX idx_contract_process ON contract_info (process_instance_id);
CREATE INDEX idx_contract_wf_inst ON contract_info (wf_instance_id);
