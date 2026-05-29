-- ==================== 协作功能 ====================

-- 讨论评论表
CREATE TABLE IF NOT EXISTS discussion_comment (
    id BIGINT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    entity_id BIGINT NOT NULL COMMENT '实体ID',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT COMMENT '父评论ID',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    author_name VARCHAR(100) COMMENT '作者姓名',
    mentions VARCHAR(500) COMMENT '提及的用户',
    deleted TINYINT NOT NULL DEFAULT 0,
    deleted_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_comment_entity (entity_type, entity_id),
    INDEX idx_comment_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讨论评论表';

-- 离线审批缓存表
CREATE TABLE IF NOT EXISTS offline_approval_cache (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    action VARCHAR(20) NOT NULL COMMENT '操作类型',
    comment VARCHAR(500) COMMENT '审批意见',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SYNCED/FAILED',
    operation_data TEXT COMMENT '操作数据(JSON)',
    error_message VARCHAR(500) COMMENT '错误信息',
    synced_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_offline_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离线审批缓存表';

-- ==================== 自动化功能 ====================

-- 自动化规则表
CREATE TABLE IF NOT EXISTS automation_rule (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    trigger_type VARCHAR(50) NOT NULL COMMENT '触发类型',
    condition_expr VARCHAR(500) COMMENT '条件表达式',
    action_expr VARCHAR(50) NOT NULL COMMENT '执行动作',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_rule_trigger (trigger_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化规则表';

-- 自动化执行记录表
CREATE TABLE IF NOT EXISTS automation_execution (
    id BIGINT PRIMARY KEY,
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED',
    error_message VARCHAR(500) COMMENT '错误信息',
    executed_at DATETIME NOT NULL,
    INDEX idx_execution_rule (rule_id, executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化执行记录表';

-- ==================== 合规检查 ====================

-- 合规违规记录表
CREATE TABLE IF NOT EXISTS compliance_violation (
    id BIGINT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    entity_id BIGINT NOT NULL COMMENT '实体ID',
    violation_type VARCHAR(50) NOT NULL COMMENT '违规类型',
    description VARCHAR(500) COMMENT '违规描述',
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '严重程度',
    checked_by BIGINT COMMENT '检查人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_violation_entity (entity_type, entity_id),
    INDEX idx_violation_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规违规记录表';

-- ==================== 数据生命周期 ====================

-- 数据归档记录表
CREATE TABLE IF NOT EXISTS data_archive_log (
    id BIGINT PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    record_count INT NOT NULL COMMENT '归档记录数',
    archive_date DATE NOT NULL COMMENT '归档日期',
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS/FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据归档记录表';
