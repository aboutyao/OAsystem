CREATE TABLE wf_cc_record (
    id BIGINT NOT NULL,
    wf_instance_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    cc_reason VARCHAR(500),
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    PRIMARY KEY (id)
);

CREATE INDEX idx_wf_cc_inst ON wf_cc_record (wf_instance_id);
CREATE INDEX idx_wf_cc_receiver ON wf_cc_record (receiver_id);
CREATE INDEX idx_wf_cc_created ON wf_cc_record (created_at);

CREATE TABLE wf_delegation (
    id BIGINT NOT NULL,
    delegator_id BIGINT NOT NULL,
    delegatee_id BIGINT NOT NULL,
    business_scope VARCHAR(64),
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    cancelled_at DATETIME,
    PRIMARY KEY (id)
);

CREATE INDEX idx_wf_delegation_delegator ON wf_delegation (delegator_id, status);
CREATE INDEX idx_wf_delegation_delegatee ON wf_delegation (delegatee_id, status);
CREATE INDEX idx_wf_delegation_period ON wf_delegation (start_at, end_at);

ALTER TABLE wf_task ADD COLUMN add_sign_origin_task_id BIGINT;
ALTER TABLE wf_task ADD COLUMN add_sign_mode VARCHAR(16);
ALTER TABLE wf_task DROP CONSTRAINT uk_wf_task_flowable;
CREATE INDEX idx_wf_task_flowable ON wf_task (flowable_task_id);
CREATE INDEX idx_wf_task_origin ON wf_task (add_sign_origin_task_id);
