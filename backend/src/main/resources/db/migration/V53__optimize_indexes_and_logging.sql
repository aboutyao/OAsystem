-- ==================== 索引优化 ====================
-- 请假表：按状态+创建时间查询频繁
CREATE INDEX IF NOT EXISTS idx_oa_leave_status_created ON oa_leave(status, created_at);
CREATE INDEX IF NOT EXISTS idx_oa_leave_creator ON oa_leave(created_by, status);

-- 报销表：按状态+创建时间查询频繁
CREATE INDEX IF NOT EXISTS idx_oa_expense_status_created ON oa_expense(status, created_at);
CREATE INDEX IF NOT EXISTS idx_oa_expense_creator ON oa_expense(created_by, status);

-- 采购表：按状态+创建时间查询频繁
CREATE INDEX IF NOT EXISTS idx_oa_purchase_status_created ON oa_purchase(status, created_at);

-- 印章申请表：按状态+创建时间查询频繁
CREATE INDEX IF NOT EXISTS idx_oa_seal_status_created ON oa_seal_apply(status, created_at);

-- 合同表：按状态+到期日查询频繁
CREATE INDEX IF NOT EXISTS idx_contract_status ON contract_info(status, deleted);
CREATE INDEX IF NOT EXISTS idx_contract_expiry ON contract_info(end_date, status);

-- 工作流实例：按状态+创建时间查询频繁
CREATE INDEX IF NOT EXISTS idx_wf_instance_status ON wf_process_instance(status, created_at);
CREATE INDEX IF NOT EXISTS idx_wf_instance_starter ON wf_process_instance(starter_id, status);

-- 工作流任务：按分配人+状态查询频繁（待办列表）
CREATE INDEX IF NOT EXISTS idx_wf_task_assignee_status ON wf_task(assignee_id, status);
CREATE INDEX IF NOT EXISTS idx_wf_task_instance ON wf_task(wf_instance_id);

-- 任务记录：按操作人查询频繁
CREATE INDEX IF NOT EXISTS idx_wf_task_record_operator ON wf_task_record(operator_id, created_at);

-- 消息表：按接收人+已读状态查询频繁
CREATE INDEX IF NOT EXISTS idx_msg_receiver_read ON msg_message(receiver_id, is_read);

-- 预算表：按部门+类别+年月查询频繁
CREATE INDEX IF NOT EXISTS idx_budget_lookup ON oa_budget(dept_id, category, year, month, status);

-- 余额表：按用户+类型+年份查询频繁
CREATE INDEX IF NOT EXISTS idx_leave_balance_lookup ON oa_leave_balance(user_id, leave_type, year);

-- 审计日志：按操作人+时间查询频繁
CREATE INDEX IF NOT EXISTS idx_audit_operation_operator ON audit_operation_log(operator_id, created_at);

-- ==================== 幂等性支持 ====================
-- 幂等性检查表
CREATE TABLE IF NOT EXISTS idempotency_key (
    id BIGINT PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE COMMENT '幂等键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    request_path VARCHAR(255) NOT NULL COMMENT '请求路径',
    response_body TEXT COMMENT '响应体',
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/COMPLETED/FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    INDEX idx_idempotency_key (idempotency_key),
    INDEX idx_idempotency_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等性检查表';

-- ==================== 日志追踪 ====================
-- 请求追踪表（可选，用于审计）
CREATE TABLE IF NOT EXISTS request_trace (
    id BIGINT PRIMARY KEY,
    trace_id VARCHAR(64) NOT NULL COMMENT '追踪ID',
    user_id BIGINT COMMENT '用户ID',
    method VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    path VARCHAR(255) NOT NULL COMMENT '请求路径',
    status_code INT COMMENT '响应状态码',
    duration_ms BIGINT COMMENT '耗时(ms)',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT 'User-Agent',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trace_id (trace_id),
    INDEX idx_trace_user (user_id, created_at),
    INDEX idx_trace_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请求追踪表';
