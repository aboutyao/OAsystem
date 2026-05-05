-- V25: Seed workflow process templates and published versions for the 5 BPMN definitions

-- ============================================================
-- 1. Process Templates
-- ============================================================
INSERT INTO wf_process_template (id, template_code, template_name, business_type, description, status, created_at, updated_at) VALUES
(2, 'LEAVE_APPROVAL', '请假审批流程', 'LEAVE', '按天数分级：≤1天HR备案，1-3天部门主管+HR，>3天部门主管+副总+HR', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'EXPENSE_APPROVAL', '报销审批流程', 'EXPENSE', '按金额分级：≤5000财务审核，5000-10000部门主管+财务，>10000部门主管+总经理+财务', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'SEAL_APPROVAL', '用章审批流程', 'SEAL', '按重要程度：普通行政审批，重要需部门主管+行政审批', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'PURCHASE_APPROVAL', '采购审批流程', 'PURCHASE', '按金额分级：≤5000行政审批，5000-50000部门主管+行政，>50000部门主管+总经理+行政', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'CONTRACT_APPROVAL', '合同审批流程', 'CONTRACT', '按金额分级：≤10万合同管理员审核，>10万部门主管+合同管理员+总经理', 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ============================================================
-- 2. Process Versions (all published, linked to Flowable definition keys)
-- ============================================================
INSERT INTO wf_process_version (id, template_id, version_no, flowable_definition_id, status, published_at, published_by, change_reason) VALUES
(2, 2, 1, 'oa_leave_approval', 'PUBLISHED', CURRENT_TIMESTAMP, 1, '初始版本'),
(3, 3, 1, 'oa_expense_approval', 'PUBLISHED', CURRENT_TIMESTAMP, 1, '初始版本'),
(4, 4, 1, 'oa_seal_approval', 'PUBLISHED', CURRENT_TIMESTAMP, 1, '初始版本'),
(5, 5, 1, 'oa_purchase_approval', 'PUBLISHED', CURRENT_TIMESTAMP, 1, '初始版本'),
(6, 6, 1, 'contract_approval', 'PUBLISHED', CURRENT_TIMESTAMP, 1, '初始版本');
