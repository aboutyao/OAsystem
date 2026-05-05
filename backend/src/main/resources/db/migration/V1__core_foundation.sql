CREATE TABLE sys_config (
    id BIGINT NOT NULL,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(32) NOT NULL,
    config_group VARCHAR(64),
    description VARCHAR(255),
    editable TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);

CREATE INDEX idx_sys_config_group ON sys_config (config_group);

CREATE TABLE sys_dict_type (
    id BIGINT NOT NULL,
    dict_code VARCHAR(64) NOT NULL,
    dict_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_dict_type_code UNIQUE (dict_code)
);

CREATE TABLE sys_dict_item (
    id BIGINT NOT NULL,
    dict_type_id BIGINT NOT NULL,
    item_label VARCHAR(128) NOT NULL,
    item_value VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT uk_sys_dict_item_value UNIQUE (dict_type_id, item_value)
);

CREATE INDEX idx_sys_dict_item_type ON sys_dict_item (dict_type_id);

CREATE TABLE org_dept (
    id BIGINT NOT NULL,
    parent_id BIGINT,
    dept_code VARCHAR(64) NOT NULL,
    dept_name VARCHAR(128) NOT NULL,
    dept_path VARCHAR(1000) NOT NULL,
    leader_user_id BIGINT,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_org_dept_code UNIQUE (dept_code)
);

CREATE INDEX idx_org_dept_parent ON org_dept (parent_id);
CREATE INDEX idx_org_dept_path ON org_dept (dept_path(255));
CREATE INDEX idx_org_dept_leader ON org_dept (leader_user_id);

CREATE TABLE org_position (
    id BIGINT NOT NULL,
    position_code VARCHAR(64) NOT NULL,
    position_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT uk_org_position_code UNIQUE (position_code)
);

CREATE TABLE org_rank (
    id BIGINT NOT NULL,
    rank_code VARCHAR(64) NOT NULL,
    rank_name VARCHAR(128) NOT NULL,
    rank_level INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT uk_org_rank_code UNIQUE (rank_code)
);

CREATE TABLE org_user (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    employee_no VARCHAR(64) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    mobile VARCHAR(32),
    email VARCHAR(128),
    main_dept_id BIGINT,
    position_id BIGINT,
    rank_id BIGINT,
    manager_user_id BIGINT,
    employee_status VARCHAR(32) NOT NULL,
    account_status VARCHAR(32) NOT NULL,
    entry_date DATE,
    resign_date DATE,
    last_login_at DATETIME,
    password_changed_at DATETIME,
    login_fail_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_org_user_username UNIQUE (username),
    CONSTRAINT uk_org_user_employee_no UNIQUE (employee_no)
);

CREATE INDEX idx_org_user_mobile ON org_user (mobile);
CREATE INDEX idx_org_user_main_dept ON org_user (main_dept_id);
CREATE INDEX idx_org_user_manager ON org_user (manager_user_id);
CREATE INDEX idx_org_user_employee_status ON org_user (employee_status);
CREATE INDEX idx_org_user_account_status ON org_user (account_status);

CREATE TABLE org_user_dept (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    start_date DATE,
    end_date DATE,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_org_user_dept_relation UNIQUE (user_id, dept_id, relation_type)
);

CREATE INDEX idx_org_user_dept_user ON org_user_dept (user_id);
CREATE INDEX idx_org_user_dept_dept ON org_user_dept (dept_id);

CREATE TABLE org_change_log (
    id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    change_type VARCHAR(64) NOT NULL,
    before_data JSON,
    after_data JSON,
    reason VARCHAR(500),
    operator_id BIGINT,
    operated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_org_change_target ON org_change_log (target_type, target_id);
CREATE INDEX idx_org_change_operated_at ON org_change_log (operated_at);

CREATE TABLE perm_role (
    id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    role_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_perm_role_code UNIQUE (role_code)
);

CREATE TABLE perm_menu (
    id BIGINT NOT NULL,
    parent_id BIGINT,
    menu_code VARCHAR(128) NOT NULL,
    menu_name VARCHAR(128) NOT NULL,
    route_path VARCHAR(255),
    component VARCHAR(255),
    icon VARCHAR(64),
    sort_order INT NOT NULL DEFAULT 0,
    visible TINYINT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_perm_menu_code UNIQUE (menu_code)
);

CREATE INDEX idx_perm_menu_parent ON perm_menu (parent_id);

CREATE TABLE perm_button (
    id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    button_code VARCHAR(128) NOT NULL,
    button_name VARCHAR(128) NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_perm_button_code UNIQUE (permission_code)
);

CREATE INDEX idx_perm_button_menu ON perm_button (menu_id);

CREATE TABLE perm_user_role (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_perm_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_perm_user_role_user ON perm_user_role (user_id);
CREATE INDEX idx_perm_user_role_role ON perm_user_role (role_id);

CREATE TABLE perm_role_menu (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_perm_role_menu UNIQUE (role_id, menu_id)
);

CREATE TABLE perm_role_button (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    button_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_perm_role_button UNIQUE (role_id, button_id)
);

CREATE TABLE perm_data_scope (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_perm_data_scope_role ON perm_data_scope (role_id);
CREATE INDEX idx_perm_data_scope_business ON perm_data_scope (business_type);

CREATE TABLE perm_data_scope_dept (
    id BIGINT NOT NULL,
    data_scope_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_perm_data_scope_dept UNIQUE (data_scope_id, dept_id)
);

CREATE TABLE perm_field_permission (
    id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    field_code VARCHAR(128) NOT NULL,
    visible TINYINT NOT NULL DEFAULT 1,
    editable TINYINT NOT NULL DEFAULT 0,
    required TINYINT NOT NULL DEFAULT 0,
    masked TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_perm_field_permission UNIQUE (role_id, business_type, field_code)
);

CREATE INDEX idx_perm_field_role ON perm_field_permission (role_id);
CREATE INDEX idx_perm_field_business ON perm_field_permission (business_type);

CREATE TABLE perm_temp_auth (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    auth_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_perm_temp_user ON perm_temp_auth (user_id);
CREATE INDEX idx_perm_temp_time ON perm_temp_auth (start_at, end_at);
CREATE INDEX idx_perm_temp_status ON perm_temp_auth (status);

CREATE TABLE audit_login_log (
    id BIGINT NOT NULL,
    user_id BIGINT,
    username VARCHAR(64),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    login_result VARCHAR(32) NOT NULL,
    fail_reason VARCHAR(500),
    logged_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_login_user ON audit_login_log (user_id);
CREATE INDEX idx_audit_login_username ON audit_login_log (username);
CREATE INDEX idx_audit_login_logged_at ON audit_login_log (logged_at);

CREATE TABLE audit_operation_log (
    id BIGINT NOT NULL,
    request_id VARCHAR(64),
    operator_id BIGINT,
    operation_type VARCHAR(64) NOT NULL,
    business_type VARCHAR(64),
    business_id BIGINT,
    request_method VARCHAR(16),
    request_uri VARCHAR(500),
    request_params JSON,
    result VARCHAR(32) NOT NULL,
    error_message VARCHAR(1000),
    ip_address VARCHAR(64),
    operated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_operation_request ON audit_operation_log (request_id);
CREATE INDEX idx_audit_operation_operator ON audit_operation_log (operator_id);
CREATE INDEX idx_audit_operation_business ON audit_operation_log (business_type, business_id);
CREATE INDEX idx_audit_operation_operated_at ON audit_operation_log (operated_at);
