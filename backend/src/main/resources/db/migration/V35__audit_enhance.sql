ALTER TABLE audit_operation_log ADD COLUMN old_value TEXT DEFAULT NULL COMMENT '变更前值(JSON)';
ALTER TABLE audit_operation_log ADD COLUMN new_value TEXT DEFAULT NULL COMMENT '变更后值(JSON)';
