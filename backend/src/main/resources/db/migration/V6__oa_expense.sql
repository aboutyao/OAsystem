CREATE TABLE oa_expense (
    id BIGINT NOT NULL,
    process_instance_id VARCHAR(128),
    wf_instance_id BIGINT,
    rule_version_id BIGINT,
    expense_no VARCHAR(64) NOT NULL,
    expense_type VARCHAR(64) NOT NULL,
    total_amount DECIMAL(14, 2) NOT NULL,
    paid_amount DECIMAL(14, 2),
    payee_account VARCHAR(255),
    payment_status VARCHAR(32) NOT NULL,
    paid_at DATETIME,
    reason VARCHAR(1000),
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
    CONSTRAINT uk_oa_expense_no UNIQUE (expense_no)
);

CREATE INDEX idx_oa_expense_created_by ON oa_expense (created_by);
CREATE INDEX idx_oa_expense_dept ON oa_expense (created_dept_id);
CREATE INDEX idx_oa_expense_status ON oa_expense (status);
CREATE INDEX idx_oa_expense_payment ON oa_expense (payment_status);
CREATE INDEX idx_oa_expense_process ON oa_expense (process_instance_id);
CREATE INDEX idx_oa_expense_wf_inst ON oa_expense (wf_instance_id);

CREATE TABLE oa_expense_item (
    id BIGINT NOT NULL,
    expense_id BIGINT NOT NULL,
    fee_type VARCHAR(64) NOT NULL,
    fee_date DATE NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    description VARCHAR(500),
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_oa_expense_item_expense ON oa_expense_item (expense_id);
