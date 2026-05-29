-- 预算管理表
CREATE TABLE IF NOT EXISTS oa_budget (
    id BIGINT PRIMARY KEY,
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    budget_type VARCHAR(20) NOT NULL COMMENT '预算类型: MONTHLY, QUARTERLY, YEARLY',
    year INT NOT NULL COMMENT '年份',
    month INT COMMENT '月份(月度预算)',
    quarter INT COMMENT '季度(季度预算)',
    category VARCHAR(50) NOT NULL COMMENT '预算类别: EXPENSE, PURCHASE, TRAVEL, etc.',
    budget_amount DECIMAL(15,2) NOT NULL COMMENT '预算金额',
    used_amount DECIMAL(15,2) NOT NULL DEFAULT 0 COMMENT '已使用金额',
    warning_threshold DECIMAL(5,2) NOT NULL DEFAULT 80 COMMENT '预警阈值(百分比)',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INACTIVE',
    created_by BIGINT NOT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_dept_category (dept_id, category),
    INDEX idx_year_month (year, month),
    INDEX idx_status (status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算管理表';

-- 插入示例预算数据
INSERT INTO oa_budget (id, dept_id, budget_type, year, month, category, budget_amount, used_amount, warning_threshold, status, created_by, created_at) VALUES
(1, 1, 'MONTHLY', 2026, 5, 'EXPENSE', 50000.00, 32000.00, 80, 'ACTIVE', 1, NOW()),
(2, 1, 'MONTHLY', 2026, 5, 'PURCHASE', 100000.00, 85000.00, 75, 'ACTIVE', 1, NOW()),
(3, 2, 'MONTHLY', 2026, 5, 'TRAVEL', 30000.00, 28000.00, 80, 'ACTIVE', 1, NOW()),
(4, 1, 'QUARTERLY', 2026, NULL, 'EXPENSE', 150000.00, 98000.00, 80, 'ACTIVE', 1, NOW());
