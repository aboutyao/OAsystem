CREATE TABLE asset_info (
    id BIGINT NOT NULL,
    asset_no VARCHAR(64) NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    asset_category VARCHAR(64),
    model VARCHAR(128),
    purchase_date DATE,
    purchase_amount DECIMAL(14,2),
    responsible_user_id BIGINT,
    dept_id BIGINT,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_asset_info_no UNIQUE (asset_no)
);

CREATE INDEX idx_asset_info_category ON asset_info (asset_category);
CREATE INDEX idx_asset_info_responsible ON asset_info (responsible_user_id);
CREATE INDEX idx_asset_info_dept ON asset_info (dept_id);
CREATE INDEX idx_asset_info_status ON asset_info (status);

CREATE TABLE asset_record (
    id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    record_type VARCHAR(32) NOT NULL,
    from_user_id BIGINT,
    to_user_id BIGINT,
    reason VARCHAR(500),
    operated_by BIGINT NOT NULL,
    operated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_asset_record_asset ON asset_record (asset_id);
CREATE INDEX idx_asset_record_type ON asset_record (record_type);
CREATE INDEX idx_asset_record_time ON asset_record (operated_at);

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (19, NULL, 'assets', '固定资产', '/assets', 'common/PlaceholderView', NULL, 56, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (29, 1, 19),
    (30, 3, 19);
