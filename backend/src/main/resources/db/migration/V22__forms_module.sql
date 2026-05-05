CREATE TABLE form_template (
    id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    current_version_id BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_form_template_code UNIQUE (template_code)
);

CREATE INDEX idx_form_template_business ON form_template (business_type);
CREATE INDEX idx_form_template_status ON form_template (status);

CREATE TABLE form_version (
    id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    fields_json TEXT NOT NULL,
    layout_json TEXT,
    status VARCHAR(32) NOT NULL,
    change_reason VARCHAR(500),
    published_at DATETIME,
    published_by BIGINT,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_form_version_template_no UNIQUE (template_id, version_no)
);

CREATE INDEX idx_form_version_template ON form_version (template_id);
CREATE INDEX idx_form_version_status ON form_version (status);

CREATE TABLE form_field_rule (
    id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    field_code VARCHAR(64) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    rule_expression TEXT NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_form_field_rule_template ON form_field_rule (template_id);
CREATE INDEX idx_form_field_rule_field ON form_field_rule (template_id, field_code);

CREATE TABLE form_snapshot (
    id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    data_json TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_form_snapshot_business ON form_snapshot (business_type, business_id);
CREATE INDEX idx_form_snapshot_template ON form_snapshot (template_id);

INSERT INTO form_template (
    id, template_code, template_name, business_type, description, status,
    current_version_id, created_at, updated_at, deleted
) VALUES
    (1, 'EXPENSE_FORM', '报销单', 'EXPENSE', '试用版预置报销表单', 'PUBLISHED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2, 'LEAVE_FORM', '请假单', 'LEAVE', '试用版预置请假表单', 'PUBLISHED', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO form_version (
    id, template_id, version_no, fields_json, layout_json, status, change_reason,
    published_at, published_by, created_at
) VALUES
    (1, 1, 1,
     '[{"fieldCode":"title","label":"标题","type":"text","required":true},{"fieldCode":"amount","label":"金额","type":"number","required":true},{"fieldCode":"reason","label":"事由","type":"textarea","required":false}]',
     NULL, 'PUBLISHED', '系统预置', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP),
    (2, 2, 1,
     '[{"fieldCode":"leaveType","label":"请假类型","type":"select","required":true},{"fieldCode":"startDate","label":"开始日期","type":"date","required":true},{"fieldCode":"endDate","label":"结束日期","type":"date","required":true},{"fieldCode":"reason","label":"事由","type":"textarea","required":false}]',
     NULL, 'PUBLISHED', '系统预置', CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP);
