CREATE TABLE file_library_folder (
    id BIGINT NOT NULL,
    parent_id BIGINT,
    folder_name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_file_library_folder_parent ON file_library_folder (parent_id);
CREATE INDEX idx_file_library_folder_status ON file_library_folder (status);

CREATE TABLE file_info (
    id BIGINT NOT NULL,
    folder_id BIGINT,
    file_name VARCHAR(255) NOT NULL,
    storage_name VARCHAR(255),
    file_ext VARCHAR(32),
    mime_type VARCHAR(128),
    file_size BIGINT,
    storage_type VARCHAR(32),
    storage_path VARCHAR(1000),
    checksum VARCHAR(128),
    upload_user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_file_info_folder ON file_info (folder_id);
CREATE INDEX idx_file_info_upload_user ON file_info (upload_user_id);
CREATE INDEX idx_file_info_status ON file_info (status);
CREATE INDEX idx_file_info_created ON file_info (created_at);

CREATE TABLE file_download_log (
    id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    business_type VARCHAR(64),
    business_id BIGINT,
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    downloaded_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_file_download_log_file ON file_download_log (file_id);
CREATE INDEX idx_file_download_log_user ON file_download_log (user_id);
CREATE INDEX idx_file_download_log_at ON file_download_log (downloaded_at);

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (22, NULL, 'files', '文件资料库', '/files', 'common/PlaceholderView', NULL, 58, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (35, 1, 22),
    (36, 3, 22);
