-- 补全 SYSTEM_ADMIN 和 EMPLOYEE 角色的功能权限
-- 注意: 已有的 ID 不可重复使用，从最高 ID 之后开始

-- ============ 新增 OA 相关按钮权限 ============
INSERT INTO perm_button (id, menu_id, button_code, button_name, permission_code, status) VALUES
(67, 13, 'apply',   '发起请假',   'leave:apply',          'ENABLED'),
(68, 14, 'apply',   '发起报销',   'expense:apply',        'ENABLED'),
(69, 15, 'apply',   '发起用章',   'seal:apply',           'ENABLED'),
(70, 16, 'apply',   '发起采购',   'purchase:apply',       'ENABLED'),
(71, 18, 'book',    '预订会议',   'meeting:book',         'ENABLED'),
(72, 8,  'sign',    '签署合同',   'contract:sign',        'ENABLED');

-- ============ EMPLOYEE 缺失的菜单 (role_id=3) ============
-- 补充父级菜单和子菜单
INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
-- 父级菜单 (让员工看到模块分组)
(128, 3, 7),    -- OA 业务 (父)
(129, 3, 15),   -- 用章申请
(130, 3, 16),   -- 采购申请
(131, 3, 18),   -- 会议室
(132, 3, 19),   -- 固定资产
(133, 3, 20),   -- 办公用品
(134, 3, 22),   -- 文件资料库
-- 账户管理
(135, 3, 55),   -- 个人信息
(136, 3, 56),   -- 修改密码
-- 账户父菜单 (V26 设置 parent_id=57)
(137, 3, 57);   -- 账户管理 (父)

-- ============ EMPLOYEE 缺失的按钮权限 ============
INSERT INTO perm_role_button (id, role_id, button_id) VALUES
-- 工作流审批操作
(59,  3, 42),   -- workflow:task:approve
(60,  3, 43),   -- workflow:task:reject
(61,  3, 44),   -- workflow:task:transfer
(62,  3, 45),   -- workflow:task:add-sign
(63,  3, 46),   -- workflow:task:remind
-- OA 发起申请
(64,  3, 67),   -- leave:apply
(65,  3, 68),   -- expense:apply
(66,  3, 69),   -- seal:apply
(67,  3, 70),   -- purchase:apply
(68,  3, 71),   -- meeting:book
-- 合同
(69,  3, 37),   -- contract:create
(70,  3, 40),   -- contract:submit
(71,  3, 72);   -- contract:sign

-- ============ SYSTEM_ADMIN 补充缺失菜单 (role_id=2) ============
INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
(138, 2, 1),    -- 工作台
(139, 2, 2),    -- 我的待办
(140, 2, 7),    -- OA 业务
(141, 2, 8),    -- 合同管理
(142, 2, 9),    -- 报表统计
(143, 2, 12),   -- 通知公告
(144, 2, 18),   -- 会议室
(145, 2, 19),   -- 固定资产
(146, 2, 20),   -- 办公用品
(147, 2, 21),   -- 消息中心
(148, 2, 22),   -- 文件资料库
(149, 2, 55),   -- 个人信息
(150, 2, 56),   -- 修改密码
(151, 2, 57);   -- 账户管理

-- SYSTEM_ADMIN 补充按钮
INSERT INTO perm_role_button (id, role_id, button_id) VALUES
(72,  2, 42),   -- workflow:task:approve
(73,  2, 43),   -- workflow:task:reject
(74,  2, 44),   -- workflow:task:transfer
(75,  2, 7),    -- audit:view
(76,  2, 14),   -- report:view (if exists)
(77,  2, 62);   -- export:batch:create
