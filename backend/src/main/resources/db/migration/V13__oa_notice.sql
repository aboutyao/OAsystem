CREATE TABLE oa_notice (
    id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'GENERAL',
    publish_scope_type VARCHAR(32) NOT NULL DEFAULT 'ALL',
    top_flag TINYINT NOT NULL DEFAULT 0,
    top_until DATETIME,
    publish_at DATETIME,
    withdraw_at DATETIME,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE INDEX idx_oa_notice_status ON oa_notice (status);
CREATE INDEX idx_oa_notice_publish_at ON oa_notice (publish_at);
CREATE INDEX idx_oa_notice_category ON oa_notice (category);
CREATE INDEX idx_oa_notice_created_by ON oa_notice (created_by);

CREATE TABLE oa_notice_read (
    id BIGINT NOT NULL,
    notice_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at DATETIME NOT NULL,
    confirmed TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_notice_read_user UNIQUE (notice_id, user_id)
);

CREATE INDEX idx_oa_notice_read_user ON oa_notice_read (user_id);
