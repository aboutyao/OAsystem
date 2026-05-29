-- ==================== 集成功能 ====================

-- 企业微信/钉钉配置表
CREATE TABLE IF NOT EXISTS integration_config (
    id BIGINT PRIMARY KEY,
    integration_type VARCHAR(50) NOT NULL COMMENT '集成类型: WECHAT_WORK/DINGTALK',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    UNIQUE KEY uk_integration_config (integration_type, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集成配置表';

-- ==================== API开放平台 ====================

-- API应用表
CREATE TABLE IF NOT EXISTS open_api_app (
    id BIGINT PRIMARY KEY,
    app_name VARCHAR(100) NOT NULL COMMENT '应用名称',
    description VARCHAR(500) COMMENT '应用描述',
    app_key VARCHAR(64) NOT NULL UNIQUE COMMENT '应用Key',
    app_secret VARCHAR(64) NOT NULL COMMENT '应用Secret',
    callback_url VARCHAR(500) COMMENT '回调URL',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    call_count BIGINT NOT NULL DEFAULT 0 COMMENT '调用次数',
    last_used_at DATETIME COMMENT '最后使用时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_app_key (app_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API应用表';

-- API访问令牌表
CREATE TABLE IF NOT EXISTS open_api_token (
    id BIGINT PRIMARY KEY,
    app_id BIGINT NOT NULL COMMENT '应用ID',
    access_token VARCHAR(128) NOT NULL UNIQUE COMMENT '访问令牌',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_token (access_token, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API访问令牌表';

-- API调用日志表
CREATE TABLE IF NOT EXISTS open_api_log (
    id BIGINT PRIMARY KEY,
    app_id BIGINT NOT NULL COMMENT '应用ID',
    request_path VARCHAR(255) NOT NULL COMMENT '请求路径',
    request_method VARCHAR(10) NOT NULL COMMENT '请求方法',
    status_code INT COMMENT '响应状态码',
    response_time_ms BIGINT COMMENT '响应时间(ms)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_log_app (app_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API调用日志表';

-- ==================== 个性化仪表盘 ====================

-- 用户仪表盘配置表
CREATE TABLE IF NOT EXISTS user_dashboard_config (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    module_key VARCHAR(50) NOT NULL COMMENT '模块Key',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    config_data TEXT COMMENT '配置数据(JSON)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dashboard_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户仪表盘配置表';

-- 用户操作日志表（用于快捷入口排序）
CREATE TABLE IF NOT EXISTS user_action_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    action_path VARCHAR(255) NOT NULL COMMENT '操作路径',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_action_user (user_id, action_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户操作日志表';

-- ==================== 主题定制 ====================

-- 系统主题配置表
CREATE TABLE IF NOT EXISTS system_theme_config (
    id BIGINT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统主题配置表';
