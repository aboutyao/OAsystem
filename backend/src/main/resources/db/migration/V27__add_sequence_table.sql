-- V27__add_sequence_table.sql
-- Create sys_sequence table for atomic ID generation to fix race conditions in nextId() pattern.

CREATE TABLE sys_sequence (
    seq_name VARCHAR(64) NOT NULL PRIMARY KEY,
    current_value BIGINT NOT NULL DEFAULT 0
);

-- Seed from current MAX(id) of every affected table so nextId() picks up where we left off.
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'sys_config', COALESCE(MAX(id), 0) FROM sys_config;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'sys_dict_type', COALESCE(MAX(id), 0) FROM sys_dict_type;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'sys_dict_item', COALESCE(MAX(id), 0) FROM sys_dict_item;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'org_dept', COALESCE(MAX(id), 0) FROM org_dept;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'org_position', COALESCE(MAX(id), 0) FROM org_position;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'org_rank', COALESCE(MAX(id), 0) FROM org_rank;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'org_user', COALESCE(MAX(id), 0) FROM org_user;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'org_user_dept', COALESCE(MAX(id), 0) FROM org_user_dept;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'org_change_log', COALESCE(MAX(id), 0) FROM org_change_log;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_role', COALESCE(MAX(id), 0) FROM perm_role;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_menu', COALESCE(MAX(id), 0) FROM perm_menu;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_button', COALESCE(MAX(id), 0) FROM perm_button;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_user_role', COALESCE(MAX(id), 0) FROM perm_user_role;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_role_menu', COALESCE(MAX(id), 0) FROM perm_role_menu;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_role_button', COALESCE(MAX(id), 0) FROM perm_role_button;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_data_scope', COALESCE(MAX(id), 0) FROM perm_data_scope;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_data_scope_dept', COALESCE(MAX(id), 0) FROM perm_data_scope_dept;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_field_permission', COALESCE(MAX(id), 0) FROM perm_field_permission;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'perm_temp_auth', COALESCE(MAX(id), 0) FROM perm_temp_auth;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'audit_login_log', COALESCE(MAX(id), 0) FROM audit_login_log;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'audit_operation_log', COALESCE(MAX(id), 0) FROM audit_operation_log;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'wf_process_template', COALESCE(MAX(id), 0) FROM wf_process_template;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'wf_process_version', COALESCE(MAX(id), 0) FROM wf_process_version;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'wf_process_instance', COALESCE(MAX(id), 0) FROM wf_process_instance;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'wf_task', COALESCE(MAX(id), 0) FROM wf_task;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'wf_task_record', COALESCE(MAX(id), 0) FROM wf_task_record;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'wf_cc_record', COALESCE(MAX(id), 0) FROM wf_cc_record;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'wf_delegation', COALESCE(MAX(id), 0) FROM wf_delegation;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'rule_definition', COALESCE(MAX(id), 0) FROM rule_definition;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'rule_group', COALESCE(MAX(id), 0) FROM rule_group;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'rule_version', COALESCE(MAX(id), 0) FROM rule_version;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'rule_audit_log', COALESCE(MAX(id), 0) FROM rule_audit_log;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_leave', COALESCE(MAX(id), 0) FROM oa_leave;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_expense', COALESCE(MAX(id), 0) FROM oa_expense;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_expense_item', COALESCE(MAX(id), 0) FROM oa_expense_item;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_seal_apply', COALESCE(MAX(id), 0) FROM oa_seal_apply;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_purchase', COALESCE(MAX(id), 0) FROM oa_purchase;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_purchase_item', COALESCE(MAX(id), 0) FROM oa_purchase_item;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'contract_info', COALESCE(MAX(id), 0) FROM contract_info;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'asset_info', COALESCE(MAX(id), 0) FROM asset_info;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'asset_record', COALESCE(MAX(id), 0) FROM asset_record;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'asset_supply', COALESCE(MAX(id), 0) FROM asset_supply;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'asset_supply_record', COALESCE(MAX(id), 0) FROM asset_supply_record;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'msg_message', COALESCE(MAX(id), 0) FROM msg_message;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'sys_import_task', COALESCE(MAX(id), 0) FROM sys_import_task;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'sys_export_task', COALESCE(MAX(id), 0) FROM sys_export_task;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'sys_number_rule', COALESCE(MAX(id), 0) FROM sys_number_rule;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'sys_work_calendar', COALESCE(MAX(id), 0) FROM sys_work_calendar;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'form_template', COALESCE(MAX(id), 0) FROM form_template;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'form_version', COALESCE(MAX(id), 0) FROM form_version;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'form_field_rule', COALESCE(MAX(id), 0) FROM form_field_rule;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'form_snapshot', COALESCE(MAX(id), 0) FROM form_snapshot;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'file_library_folder', COALESCE(MAX(id), 0) FROM file_library_folder;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'file_info', COALESCE(MAX(id), 0) FROM file_info;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'meeting_room', COALESCE(MAX(id), 0) FROM meeting_room;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'meeting_booking', COALESCE(MAX(id), 0) FROM meeting_booking;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_notice', COALESCE(MAX(id), 0) FROM oa_notice;
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_notice_read', COALESCE(MAX(id), 0) FROM oa_notice_read;
