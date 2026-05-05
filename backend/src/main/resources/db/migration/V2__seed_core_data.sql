INSERT INTO sys_config (
    id, config_key, config_value, config_type, config_group, description, editable, created_at, updated_at
) VALUES
    (1, 'security.password.minLength', '8', 'NUMBER', 'security', '密码最小长度', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'security.login.maxFailCount', '5', 'NUMBER', 'security', '登录失败锁定次数', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'security.login.lockMinutes', '15', 'NUMBER', 'security', '登录锁定分钟数', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'file.upload.maxSizeMb', '50', 'NUMBER', 'file', '单文件上传大小限制 MB', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'paging.defaultSize', '20', 'NUMBER', 'ui', '默认分页大小', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 'paging.maxSize', '100', 'NUMBER', 'ui', '最大分页大小', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_dict_type (
    id, dict_code, dict_name, status, remark, created_at, updated_at
) VALUES
    (1, 'common_status', '通用启停状态', 'ENABLED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'employee_status', '员工状态', 'ENABLED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'business_status', '业务单据状态', 'ENABLED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'data_scope', '数据权限范围', 'ENABLED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_dict_item (
    id, dict_type_id, item_label, item_value, sort_order, status, remark
) VALUES
    (1, 1, '启用', 'ENABLED', 1, 'ENABLED', NULL),
    (2, 1, '停用', 'DISABLED', 2, 'ENABLED', NULL),
    (3, 2, '试用', 'PROBATION', 1, 'ENABLED', NULL),
    (4, 2, '正式', 'ACTIVE', 2, 'ENABLED', NULL),
    (5, 2, '离职', 'RESIGNED', 3, 'ENABLED', NULL),
    (6, 3, '草稿', 'DRAFT', 1, 'ENABLED', NULL),
    (7, 3, '审批中', 'APPROVING', 2, 'ENABLED', NULL),
    (8, 3, '已通过', 'APPROVED', 3, 'ENABLED', NULL),
    (9, 3, '已驳回', 'REJECTED', 4, 'ENABLED', NULL),
    (10, 4, '仅本人', 'SELF', 1, 'ENABLED', NULL),
    (11, 4, '本部门', 'DEPT', 2, 'ENABLED', NULL),
    (12, 4, '本部门及下级', 'DEPT_AND_CHILD', 3, 'ENABLED', NULL),
    (13, 4, '全部数据', 'ALL', 4, 'ENABLED', NULL);

INSERT INTO org_dept (
    id, parent_id, dept_code, dept_name, dept_path, leader_user_id, sort_order, status, created_at, updated_at, deleted
) VALUES
    (1, NULL, 'HQ', '总公司', '/1/', 1, 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (2, 1, 'EXEC', '总经办', '/1/2/', 1, 1, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (3, 1, 'TECH', '技术部', '/1/3/', NULL, 2, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (4, 1, 'FIN', '财务部', '/1/4/', NULL, 3, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (5, 1, 'ADMIN', '人事行政部', '/1/5/', NULL, 4, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO org_position (
    id, position_code, position_name, status, sort_order, remark
) VALUES
    (1, 'SYS_ADMIN', '系统管理员', 'ENABLED', 1, NULL),
    (2, 'EMPLOYEE', '员工', 'ENABLED', 2, NULL);

INSERT INTO org_rank (
    id, rank_code, rank_name, rank_level, status, remark
) VALUES
    (1, 'ADMIN', '管理员', 100, 'ENABLED', NULL),
    (2, 'P1', '员工', 1, 'ENABLED', NULL);

INSERT INTO org_user (
    id, username, password_hash, employee_no, real_name, mobile, email, main_dept_id, position_id,
    rank_id, manager_user_id, employee_status, account_status, entry_date, resign_date, last_login_at,
    password_changed_at, login_fail_count, locked_until, created_at, updated_at, deleted
) VALUES (
    1, 'admin', '{noop}admin123', 'E0001', '系统管理员', NULL, NULL, 2, 1,
    1, NULL, 'ACTIVE', 'ENABLED', CURRENT_DATE, NULL, NULL,
    CURRENT_TIMESTAMP, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
);

INSERT INTO org_user_dept (
    id, user_id, dept_id, relation_type, start_date, end_date, created_at
) VALUES
    (1, 1, 2, 'MAIN', CURRENT_DATE, NULL, CURRENT_TIMESTAMP);

INSERT INTO perm_role (
    id, role_code, role_name, role_type, status, sort_order, remark, created_at, updated_at
) VALUES
    (1, 'SUPER_ADMIN', '超级管理员', 'SYSTEM', 'ENABLED', 1, '系统最高权限，数量应受控', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'SYSTEM_ADMIN', '系统管理员', 'SYSTEM', 'ENABLED', 2, '系统配置、用户、组织、权限维护', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'EMPLOYEE', '普通员工', 'BUSINESS', 'ENABLED', 99, '发起申请、处理本人待办、查看本人数据', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (1, NULL, 'dashboard', '工作台', '/dashboard', 'dashboard/DashboardView', NULL, 1, 1, 'ENABLED'),
    (2, NULL, 'todos', '我的待办', '/todos', 'common/PlaceholderView', NULL, 2, 1, 'ENABLED'),
    (3, NULL, 'org', '组织人员', '/org', 'common/PlaceholderView', NULL, 10, 1, 'ENABLED'),
    (4, NULL, 'permission', '权限中心', '/permission', 'common/PlaceholderView', NULL, 20, 1, 'ENABLED'),
    (5, NULL, 'workflow', '流程中心', '/workflow', 'common/PlaceholderView', NULL, 30, 1, 'ENABLED'),
    (6, NULL, 'rules', '规则中心', '/rules', 'common/PlaceholderView', NULL, 40, 1, 'ENABLED'),
    (7, NULL, 'oa', 'OA 业务', '/oa/leaves', 'common/PlaceholderView', NULL, 50, 1, 'ENABLED'),
    (8, NULL, 'contracts', '合同管理', '/contracts', 'common/PlaceholderView', NULL, 60, 1, 'ENABLED'),
    (9, NULL, 'reports', '报表统计', '/reports', 'common/PlaceholderView', NULL, 70, 1, 'ENABLED'),
    (10, NULL, 'audit', '审计日志', '/audit', 'common/PlaceholderView', NULL, 80, 1, 'ENABLED'),
    (11, NULL, 'ops', '运维监控', '/ops', 'common/PlaceholderView', NULL, 90, 1, 'ENABLED');

INSERT INTO perm_button (
    id, menu_id, button_code, button_name, permission_code, status
) VALUES
    (1, 3, 'view', '查看', 'org:view', 'ENABLED'),
    (2, 3, 'create', '新增', 'org:create', 'ENABLED'),
    (3, 4, 'view', '查看', 'permission:view', 'ENABLED'),
    (4, 4, 'assign', '分配权限', 'permission:role:assign', 'ENABLED'),
    (5, 5, 'publish', '发布流程版本', 'workflow:version:publish', 'ENABLED'),
    (6, 6, 'publish', '发布规则版本', 'rule:version:publish', 'ENABLED'),
    (7, 10, 'view', '查看审计', 'audit:view', 'ENABLED');

INSERT INTO perm_user_role (
    id, user_id, role_id, created_at
) VALUES
    (1, 1, 1, CURRENT_TIMESTAMP);

INSERT INTO perm_role_menu (
    id, role_id, menu_id
) VALUES
    (1, 1, 1),
    (2, 1, 2),
    (3, 1, 3),
    (4, 1, 4),
    (5, 1, 5),
    (6, 1, 6),
    (7, 1, 7),
    (8, 1, 8),
    (9, 1, 9),
    (10, 1, 10),
    (11, 1, 11);

INSERT INTO perm_role_button (
    id, role_id, button_id
) VALUES
    (1, 1, 1),
    (2, 1, 2),
    (3, 1, 3),
    (4, 1, 4),
    (5, 1, 5),
    (6, 1, 6),
    (7, 1, 7);

INSERT INTO perm_data_scope (
    id, role_id, scope_type, business_type, created_at
) VALUES
    (1, 1, 'ALL', '*', CURRENT_TIMESTAMP),
    (2, 3, 'SELF', '*', CURRENT_TIMESTAMP);
