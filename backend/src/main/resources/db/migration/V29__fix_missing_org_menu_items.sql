-- V29: 补全组织模块缺失的菜单项（组织架构、用户管理）

-- 组织架构（sort_order=0）
INSERT IGNORE INTO perm_menu (id, parent_id, menu_code, menu_name, route_path, sort_order, status)
SELECT 58, 3, 'org_depts', '组织架构', '/org/depts', 0, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM perm_menu WHERE menu_code = 'org_depts');

-- 用户管理（sort_order=1）
INSERT IGNORE INTO perm_menu (id, parent_id, menu_code, menu_name, route_path, sort_order, status)
SELECT 59, 3, 'org_users', '用户管理', '/org/users', 1, 'ENABLED'
WHERE NOT EXISTS (SELECT 1 FROM perm_menu WHERE menu_code = 'org_users');

-- 绑定到角色 1（超级管理员）
INSERT IGNORE INTO perm_role_menu (id, role_id, menu_id)
SELECT (SELECT IFNULL(MAX(id), 0) + 1 FROM perm_role_menu rm1), 1, 58
WHERE NOT EXISTS (SELECT 1 FROM perm_role_menu WHERE role_id = 1 AND menu_id = 58);

INSERT IGNORE INTO perm_role_menu (id, role_id, menu_id)
SELECT (SELECT IFNULL(MAX(id), 0) + 1 FROM perm_role_menu rm2), 1, 59
WHERE NOT EXISTS (SELECT 1 FROM perm_role_menu WHERE role_id = 1 AND menu_id = 59);
