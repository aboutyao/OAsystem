-- Add EXPENSE_HIGH workflow template for high-amount expense approvals (amount > 10000).
-- This template is used when the expense totalAmount exceeds 10,000 and requires GM approval.
--
-- Note: A published BPMN process version must be created via the workflow template management UI
-- after this migration runs. The fallback logic in WorkflowService.resolvePublishedVersion()
-- will automatically fall back to the standard EXPENSE template if no EXPENSE_HIGH template
-- has been published yet.

INSERT INTO wf_process_template (id, template_code, template_name, business_type, description, status, created_at, updated_at, deleted)
SELECT nextval('wf_process_template_id_seq'), 'EXPENSE_HIGH', '高金额报销审批', 'EXPENSE_HIGH',
       '报销金额超过10000元时使用，需要总经理审批', 'ENABLED', NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM wf_process_template WHERE business_type = 'EXPENSE_HIGH'
);
