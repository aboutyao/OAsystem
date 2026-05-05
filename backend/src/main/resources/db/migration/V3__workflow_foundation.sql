CREATE TABLE wf_process_template (
    id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wf_template_code UNIQUE (template_code)
);

CREATE INDEX idx_wf_template_business ON wf_process_template (business_type);

CREATE TABLE wf_process_version (
    id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    flowable_definition_id VARCHAR(128),
    bpmn_xml LONGTEXT,
    status VARCHAR(32) NOT NULL,
    published_at DATETIME,
    published_by BIGINT,
    change_reason VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT uk_wf_template_version UNIQUE (template_id, version_no)
);

CREATE INDEX idx_wf_version_template ON wf_process_version (template_id);
CREATE INDEX idx_wf_version_flowable ON wf_process_version (flowable_definition_id);

CREATE TABLE wf_process_instance (
    id BIGINT NOT NULL,
    process_instance_id VARCHAR(128) NOT NULL,
    template_id BIGINT NOT NULL,
    process_version_id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    starter_id BIGINT NOT NULL,
    starter_name_snapshot VARCHAR(64) NOT NULL,
    starter_dept_id BIGINT,
    starter_dept_name_snapshot VARCHAR(128),
    current_node_name VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    started_at DATETIME NOT NULL,
    ended_at DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_wf_inst_process_pid UNIQUE (process_instance_id)
);

CREATE INDEX idx_wf_inst_business ON wf_process_instance (business_type, business_id);
CREATE INDEX idx_wf_inst_starter ON wf_process_instance (starter_id);
CREATE INDEX idx_wf_inst_status ON wf_process_instance (status);
CREATE INDEX idx_wf_inst_started ON wf_process_instance (started_at);

CREATE TABLE wf_task (
    id BIGINT NOT NULL,
    flowable_task_id VARCHAR(128) NOT NULL,
    process_instance_id VARCHAR(128) NOT NULL,
    wf_instance_id BIGINT NOT NULL,
    node_id VARCHAR(128) NOT NULL,
    node_name VARCHAR(128) NOT NULL,
    assignee_id BIGINT NOT NULL,
    assignee_name_snapshot VARCHAR(64) NOT NULL,
    assignee_dept_id BIGINT,
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    due_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wf_task_flowable UNIQUE (flowable_task_id)
);

CREATE INDEX idx_wf_task_wf_inst ON wf_task (wf_instance_id);
CREATE INDEX idx_wf_task_assignee ON wf_task (assignee_id, status);
CREATE INDEX idx_wf_task_process ON wf_task (process_instance_id);

CREATE TABLE wf_task_record (
    id BIGINT NOT NULL,
    wf_instance_id BIGINT NOT NULL,
    task_id BIGINT,
    action VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_name_snapshot VARCHAR(64) NOT NULL,
    node_name VARCHAR(128),
    comment VARCHAR(1000),
    attachment_ids VARCHAR(1000),
    operated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_wf_record_inst ON wf_task_record (wf_instance_id);
CREATE INDEX idx_wf_record_task ON wf_task_record (task_id);
CREATE INDEX idx_wf_record_operator ON wf_task_record (operator_id);
CREATE INDEX idx_wf_record_operated ON wf_task_record (operated_at);

INSERT INTO wf_process_template (
    id, template_code, template_name, business_type, description, status, created_at, updated_at
) VALUES (
    1, 'OA_SIMPLE_APPROVAL', '通用一级审批', 'GENERIC', '单节点经理审批（Flowable 接入示例）', 'ENABLED',
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO wf_process_version (
    id, template_id, version_no, flowable_definition_id, bpmn_xml, status, published_at, published_by, change_reason
) VALUES (
    1, 1, 1, 'oa_simple_approval', NULL, 'PUBLISHED', CURRENT_TIMESTAMP, 1, '系统预置'
);
