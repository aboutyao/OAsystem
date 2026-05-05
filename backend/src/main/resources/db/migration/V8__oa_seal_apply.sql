CREATE TABLE oa_seal_apply (
    id BIGINT NOT NULL,
    process_instance_id VARCHAR(128),
    wf_instance_id BIGINT,
    seal_type VARCHAR(64) NOT NULL,
    seal_name VARCHAR(128) NOT NULL,
    file_title VARCHAR(255) NOT NULL,
    use_reason VARCHAR(1000),
    use_at DATETIME NOT NULL,
    out_flag TINYINT NOT NULL DEFAULT 0,
    return_at DATETIME,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_name_snapshot VARCHAR(64) NOT NULL,
    created_dept_id BIGINT,
    created_dept_name_snapshot VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_oa_seal_seal_type ON oa_seal_apply (seal_type);
CREATE INDEX idx_oa_seal_created_by ON oa_seal_apply (created_by);
CREATE INDEX idx_oa_seal_status ON oa_seal_apply (status);
CREATE INDEX idx_oa_seal_process ON oa_seal_apply (process_instance_id);
CREATE INDEX idx_oa_seal_wf_inst ON oa_seal_apply (wf_instance_id);

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (15, NULL, 'oa_seals', '用章申请', '/oa/seals', 'common/PlaceholderView', NULL, 53, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (20, 1, 15),
    (21, 3, 15);
