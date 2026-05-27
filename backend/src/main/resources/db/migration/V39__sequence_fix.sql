-- Add missing sys_sequence entries for tables added after V27.

INSERT INTO sys_sequence (seq_name, current_value) SELECT 'leave_type', COALESCE(MAX(id), 0) FROM leave_type WHERE NOT EXISTS (SELECT 1 FROM sys_sequence WHERE seq_name = 'leave_type');
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'leave_balance', COALESCE(MAX(id), 0) FROM leave_balance WHERE NOT EXISTS (SELECT 1 FROM sys_sequence WHERE seq_name = 'leave_balance');
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'leave_balance_log', COALESCE(MAX(id), 0) FROM leave_balance_log WHERE NOT EXISTS (SELECT 1 FROM sys_sequence WHERE seq_name = 'leave_balance_log');
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'oa_expense_attachment', COALESCE(MAX(id), 0) FROM oa_expense_attachment WHERE NOT EXISTS (SELECT 1 FROM sys_sequence WHERE seq_name = 'oa_expense_attachment');
INSERT INTO sys_sequence (seq_name, current_value) SELECT 'app_exception_log', COALESCE(MAX(id), 0) FROM app_exception_log WHERE NOT EXISTS (SELECT 1 FROM sys_sequence WHERE seq_name = 'app_exception_log');
