-- Seed default approval roles (idempotent)
INSERT INTO perm_role (id, role_code, role_name, role_type, status, sort_order, remark, created_at, updated_at) VALUES
(1, 'HR',       '人力资源', 'SYSTEM', 'ENABLED', 10, '负责人力资源审批', NOW(), NOW()),
(2, 'GM',       '总经理',   'SYSTEM', 'ENABLED', 20, '总经理审批', NOW(), NOW()),
(3, 'CEO',      '首席执行官','SYSTEM', 'ENABLED', 30, 'CEO审批', NOW(), NOW()),
(4, 'FINANCE',  '财务',     'SYSTEM', 'ENABLED', 40, '负责财务审批', NOW(), NOW()),
(5, 'LEGAL',    '法务',     'SYSTEM', 'ENABLED', 50, '负责法务审批', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
