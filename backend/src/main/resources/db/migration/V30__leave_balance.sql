-- 假期类型配置表
CREATE TABLE IF NOT EXISTS leave_type (
    id BIGINT PRIMARY KEY,
    type_code VARCHAR(32) NOT NULL UNIQUE COMMENT '假期类型编码：ANNUAL/SICK/PERSONAL/MATERNITY/PATERNITY/MARRIAGE/BEREAVEMENT',
    type_name VARCHAR(64) NOT NULL COMMENT '假期类型名称',
    days_per_year INT NOT NULL DEFAULT 0 COMMENT '每年默认天数，0表示不限',
    is_paid TINYINT NOT NULL DEFAULT 1 COMMENT '是否带薪：1是 0否',
    requires_proof TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要证明材料：1是 0否',
    max_consecutive_days INT DEFAULT NULL COMMENT '最大连续天数限制，NULL表示不限',
    min_leave_hours DECIMAL(4,1) DEFAULT NULL COMMENT '最小请假时长（小时），NULL表示最少1天',
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) COMMENT '假期类型配置';

-- 员工假期余额表
CREATE TABLE IF NOT EXISTS leave_balance (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '员工ID',
    leave_type VARCHAR(32) NOT NULL COMMENT '假期类型编码',
    year INT NOT NULL COMMENT '年份',
    total_days DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '应有天数',
    used_days DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '已用天数',
    pending_days DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '审批中天数',
    remaining_days DECIMAL(5,1) GENERATED ALWAYS AS (total_days - used_days - pending_days) STORED COMMENT '剩余天数',
    carried_over_days DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '跨年结转天数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_type_year (user_id, leave_type, year)
) COMMENT '员工假期余额';

-- 假期余额变动记录
CREATE TABLE IF NOT EXISTS leave_balance_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    leave_type VARCHAR(32) NOT NULL,
    year INT NOT NULL,
    change_type VARCHAR(32) NOT NULL COMMENT 'GRANT/USE/CANCEL/CARRY_OVER/ADJUST',
    days DECIMAL(5,1) NOT NULL COMMENT '变动天数（正数增加，负数减少）',
    related_leave_id BIGINT COMMENT '关联的请假ID',
    remark VARCHAR(255),
    operator_id BIGINT COMMENT '操作人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) COMMENT '假期余额变动记录';
