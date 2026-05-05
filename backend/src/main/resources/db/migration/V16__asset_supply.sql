CREATE TABLE asset_supply (
    id BIGINT NOT NULL,
    supply_code VARCHAR(64) NOT NULL,
    supply_name VARCHAR(255) NOT NULL,
    category VARCHAR(64),
    unit VARCHAR(32) NOT NULL,
    stock_quantity DECIMAL(14,2) NOT NULL DEFAULT 0,
    warning_quantity DECIMAL(14,2),
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_asset_supply_code UNIQUE (supply_code)
);

CREATE INDEX idx_asset_supply_category ON asset_supply (category);
CREATE INDEX idx_asset_supply_status ON asset_supply (status);

CREATE TABLE asset_supply_record (
    id BIGINT NOT NULL,
    supply_id BIGINT NOT NULL,
    record_type VARCHAR(32) NOT NULL,
    quantity DECIMAL(14,2) NOT NULL,
    user_id BIGINT,
    reason VARCHAR(500),
    operated_by BIGINT NOT NULL,
    operated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_asset_supply_record_supply ON asset_supply_record (supply_id);
CREATE INDEX idx_asset_supply_record_type ON asset_supply_record (record_type);
CREATE INDEX idx_asset_supply_record_time ON asset_supply_record (operated_at);

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (20, NULL, 'supplies', '办公用品', '/supplies', 'common/PlaceholderView', NULL, 57, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (31, 1, 20),
    (32, 3, 20);
