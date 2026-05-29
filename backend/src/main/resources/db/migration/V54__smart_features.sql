-- ==================== 智能功能支持表 ====================

-- 任务依赖表
CREATE TABLE IF NOT EXISTS wf_task_dependency (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '当前任务ID',
    depends_on_task_id BIGINT NOT NULL COMMENT '依赖任务ID',
    dependency_type VARCHAR(20) NOT NULL DEFAULT 'FINISH_TO_START' COMMENT '依赖类型: FINISH_TO_START/START_TO_START',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COMPLETED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    INDEX idx_task_dep_task (task_id),
    INDEX idx_task_dep_depends (depends_on_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务依赖表';

-- Webhook配置表
CREATE TABLE IF NOT EXISTS webhook_config (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT 'Webhook名称',
    url VARCHAR(500) NOT NULL COMMENT '回调URL',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
    secret VARCHAR(100) COMMENT '密钥',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_webhook_event (event_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook配置表';

-- Webhook投递记录
CREATE TABLE IF NOT EXISTS webhook_delivery (
    id BIGINT PRIMARY KEY,
    webhook_id BIGINT NOT NULL COMMENT 'Webhook配置ID',
    status VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED',
    response_code INT COMMENT '响应码',
    delivered_at DATETIME NOT NULL,
    INDEX idx_delivery_webhook (webhook_id, delivered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Webhook投递记录';

-- 操作回放表
CREATE TABLE IF NOT EXISTS operation_replay (
    id BIGINT PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型',
    entity_id BIGINT NOT NULL COMMENT '实体ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    before_state TEXT COMMENT '操作前状态(JSON)',
    after_state TEXT COMMENT '操作后状态(JSON)',
    operator_id BIGINT COMMENT '操作人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_replay_entity (entity_type, entity_id),
    INDEX idx_replay_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作回放表';

-- 智能推荐记录表
CREATE TABLE IF NOT EXISTS smart_recommendation (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    recommendation_type VARCHAR(50) NOT NULL COMMENT '推荐类型',
    target_id BIGINT COMMENT '推荐目标ID',
    target_name VARCHAR(200) COMMENT '推荐目标名称',
    score DOUBLE COMMENT '推荐评分',
    reason VARCHAR(500) COMMENT '推荐理由',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_recommend_user (user_id, recommendation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能推荐记录表';
