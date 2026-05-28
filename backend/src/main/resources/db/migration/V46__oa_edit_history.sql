CREATE TABLE oa_edit_history (
    id BIGINT NOT NULL,
    entity_type VARCHAR(64) NOT NULL COMMENT '实体类型(OA_LEAVE/OA_EXPENSE/OA_PURCHASE/OA_SEAL/OA_CONTRACT)',
    entity_id BIGINT NOT NULL COMMENT '实体ID',
    snapshot_json TEXT NOT NULL COMMENT '编辑前的完整快照(JSON)',
    edited_by BIGINT NOT NULL COMMENT '编辑人ID',
    edited_at DATETIME NOT NULL COMMENT '编辑时间',
    PRIMARY KEY (id),
    INDEX idx_edit_history_entity (entity_type, entity_id),
    INDEX idx_edit_history_edited_at (edited_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OA文档编辑版本历史';
