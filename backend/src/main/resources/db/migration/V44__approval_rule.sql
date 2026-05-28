-- Dynamic approval rule engine
CREATE TABLE IF NOT EXISTS wf_approval_rule (
    id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型: LEAVE/EXPENSE/CONTRACT/PURCHASE/SEAL',
    rule_name VARCHAR(128) NOT NULL COMMENT '规则名称',
    condition_type VARCHAR(32) NOT NULL COMMENT '条件类型: AMOUNT/DAYS/DEFAULT',
    condition_value DECIMAL(12,2) COMMENT '条件值(金额/天数)',
    approval_chain JSON NOT NULL COMMENT '审批链节点列表',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级，越大越优先',
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_approval_rule_biz (business_type, status, priority DESC)
);

INSERT INTO wf_approval_rule (id, business_type, rule_name, condition_type, condition_value, approval_chain, priority, status, created_by) VALUES
(1, 'LEAVE', '请假-3天以内', 'DAYS', 3, '["DIRECT_SUPERVISOR"]', 10, 'ENABLED', 1),
(2, 'LEAVE', '请假-3-7天', 'DAYS', 7, '["DIRECT_SUPERVISOR","HR"]', 20, 'ENABLED', 1),
(3, 'LEAVE', '请假-7天以上', 'DAYS', 999, '["DIRECT_SUPERVISOR","HR","GM"]', 30, 'ENABLED', 1),
(4, 'EXPENSE', '报销-5000以内', 'AMOUNT', 5000, '["DIRECT_SUPERVISOR"]', 10, 'ENABLED', 1),
(5, 'EXPENSE', '报销-5000-20000', 'AMOUNT', 20000, '["DIRECT_SUPERVISOR","FINANCE"]', 20, 'ENABLED', 1),
(6, 'EXPENSE', '报销-20000以上', 'AMOUNT', 999999, '["DIRECT_SUPERVISOR","FINANCE","GM"]', 30, 'ENABLED', 1),
(7, 'CONTRACT', '合同-默认', 'DEFAULT', NULL, '["DIRECT_SUPERVISOR","LEGAL","GM"]', 0, 'ENABLED', 1),
(8, 'PURCHASE', '采购-5000以内', 'AMOUNT', 5000, '["DIRECT_SUPERVISOR"]', 10, 'ENABLED', 1),
(9, 'PURCHASE', '采购-5000以上', 'AMOUNT', 999999, '["DIRECT_SUPERVISOR","GM"]', 20, 'ENABLED', 1),
(10, 'SEAL', '用印-默认', 'DEFAULT', NULL, '["DIRECT_SUPERVISOR"]', 0, 'ENABLED', 1);
