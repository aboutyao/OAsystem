CREATE TABLE rule_group (
    id BIGINT NOT NULL,
    group_code VARCHAR(64) NOT NULL,
    group_name VARCHAR(128) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rule_group_code UNIQUE (group_code)
);

CREATE TABLE rule_definition (
    id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rule_def_code UNIQUE (rule_code)
);

CREATE INDEX idx_rule_def_group ON rule_definition (group_id);
CREATE INDEX idx_rule_def_business ON rule_definition (business_type);

CREATE TABLE rule_version (
    id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    rule_content TEXT NOT NULL,
    natural_language VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    effective_at DATETIME,
    expired_at DATETIME,
    published_by BIGINT,
    published_at DATETIME,
    change_reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rule_ver UNIQUE (rule_id, version_no)
);

CREATE INDEX idx_rule_ver_rule ON rule_version (rule_id);
CREATE INDEX idx_rule_ver_status ON rule_version (status);

CREATE TABLE rule_audit_log (
    id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_version_id BIGINT,
    action VARCHAR(32) NOT NULL,
    before_data TEXT,
    after_data TEXT,
    reason VARCHAR(500),
    operator_id BIGINT NOT NULL,
    operated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_rule_audit_rule ON rule_audit_log (rule_id);
CREATE INDEX idx_rule_audit_ver ON rule_audit_log (rule_version_id);
CREATE INDEX idx_rule_audit_operated ON rule_audit_log (operated_at);

INSERT INTO rule_group (id, group_code, group_name, description, status, created_at, updated_at) VALUES
    (1, 'APPROVAL_ROUTING', '审批与路由规则', '金额、时间等影响审批路径的配置', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO rule_definition (
    id, group_id, rule_code, rule_name, rule_type, business_type, description, status, created_at, updated_at
) VALUES (
    1, 1, 'EXPENSE_AMOUNT_GTE_5000', '报销金额达到阈值', 'AMOUNT', 'EXPENSE',
    '报销单金额大于等于配置值时命中（示例，用于流程分支或提示）', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO rule_definition (
    id, group_id, rule_code, rule_name, rule_type, business_type, description, status, created_at, updated_at
) VALUES (
    2, 1, 'WORKDAY_DEFAULT', '默认工作日判定', 'TIME', 'GENERIC',
    '工作时间规则占位，后续与工作日历表联动', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO rule_version (
    id, rule_id, version_no, rule_content, natural_language, status, effective_at, expired_at,
    published_by, published_at, change_reason, created_at
) VALUES (
    1, 1, 1,
    '{"type":"AMOUNT","field":"amount","operator":">=","value":5000,"message":"金额达到或超过 5000，需部门负责人审批"}',
    '当报销金额 >= 5000 时命中', 'PUBLISHED', CURRENT_TIMESTAMP, NULL,
    1, CURRENT_TIMESTAMP, '系统预置', CURRENT_TIMESTAMP
);

INSERT INTO rule_version (
    id, rule_id, version_no, rule_content, natural_language, status, effective_at, expired_at,
    published_by, published_at, change_reason, created_at
) VALUES (
    2, 2, 1,
    '{"type":"TIME","workStart":"09:00","workEnd":"18:00","timezone":"Asia/Shanghai"}',
    '默认 9:00–18:00 工作时间', 'PUBLISHED', CURRENT_TIMESTAMP, NULL,
    1, CURRENT_TIMESTAMP, '系统预置', CURRENT_TIMESTAMP
);

INSERT INTO rule_audit_log (id, rule_id, rule_version_id, action, before_data, after_data, reason, operator_id, operated_at)
VALUES (1, 1, 1, 'PUBLISH', NULL, NULL, '系统预置', 1, CURRENT_TIMESTAMP);
