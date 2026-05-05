CREATE TABLE oa_purchase (
    id BIGINT NOT NULL,
    process_instance_id VARCHAR(128),
    wf_instance_id BIGINT,
    rule_version_id BIGINT,
    purchase_no VARCHAR(64) NOT NULL,
    purchase_type VARCHAR(64) NOT NULL,
    supplier_name VARCHAR(255),
    budget_subject VARCHAR(128),
    total_amount DECIMAL(14, 2) NOT NULL,
    arrival_status VARCHAR(32) NOT NULL,
    acceptance_status VARCHAR(32) NOT NULL,
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
    CONSTRAINT uk_oa_purchase_no UNIQUE (purchase_no)
);

CREATE INDEX idx_oa_purchase_created_by ON oa_purchase (created_by);
CREATE INDEX idx_oa_purchase_dept ON oa_purchase (created_dept_id);
CREATE INDEX idx_oa_purchase_status ON oa_purchase (status);
CREATE INDEX idx_oa_purchase_process ON oa_purchase (process_instance_id);
CREATE INDEX idx_oa_purchase_wf_inst ON oa_purchase (wf_instance_id);

CREATE TABLE oa_purchase_item (
    id BIGINT NOT NULL,
    purchase_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    specification VARCHAR(255),
    quantity DECIMAL(14, 2) NOT NULL,
    unit VARCHAR(32),
    unit_price DECIMAL(14, 2) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_oa_purchase_item_purchase ON oa_purchase_item (purchase_id);

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (16, NULL, 'oa_purchases', '采购申请', '/oa/purchases', 'common/PlaceholderView', NULL, 54, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (22, 1, 16),
    (23, 3, 16);
