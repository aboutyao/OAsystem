CREATE TABLE IF NOT EXISTS oa_expense_attachment (
    id BIGINT PRIMARY KEY,
    expense_id BIGINT NOT NULL COMMENT '报销ID',
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
    file_size BIGINT NOT NULL DEFAULT 0,
    mime_type VARCHAR(128),
    uploaded_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) COMMENT '报销附件';

CREATE INDEX idx_oa_expense_attachment_expense ON oa_expense_attachment (expense_id);
