-- 补全 SYSTEM_ADMIN 和 EMPLOYEE 角色的功能权限
-- 使用 INSERT IGNORE 避免与已有数据冲突

-- ============ EMPLOYEE 补充菜单权限 (role_id=3) ============
INSERT IGNORE INTO perm_role_menu (id, role_id, menu_id) VALUES
(200, 3, 1),    -- 工作台
(201, 3, 2),    -- 我的待办
(202, 3, 7),    -- OA 业务
(203, 3, 8),    -- 合同管理
(204, 3, 9),    -- 报表统计
(205, 3, 12),   -- 通知公告
(206, 3, 15),   -- 用章申请
(207, 3, 16),   -- 采购申请
(208, 3, 18),   -- 会议室
(209, 3, 19),   -- 固定资产
(210, 3, 20),   -- 办公用品
(211, 3, 21),   -- 消息中心
(212, 3, 22),   -- 文件资料库
(213, 3, 55),   -- 个人信息
(214, 3, 56),   -- 修改密码
(215, 3, 57);   -- 账户管理

-- ============ EMPLOYEE 补充按钮权限 ============
INSERT IGNORE INTO perm_role_button (id, role_id, button_id) VALUES
(200, 3, 42),   -- workflow:task:approve
(201, 3, 43),   -- workflow:task:reject
(202, 3, 44),   -- workflow:task:transfer
(203, 3, 45),   -- workflow:task:add-sign
(204, 3, 46);   -- workflow:task:remind

-- ============ SYSTEM_ADMIN 补充菜单权限 (role_id=2) ============
INSERT IGNORE INTO perm_role_menu (id, role_id, menu_id) VALUES
(220, 2, 1),    -- 工作台
(221, 2, 2),    -- 我的待办
(222, 2, 7),    -- OA 业务
(223, 2, 8),    -- 合同管理
(224, 2, 9),    -- 报表统计
(225, 2, 12),   -- 通知公告
(226, 2, 18),   -- 会议室
(227, 2, 19),   -- 固定资产
(228, 2, 20),   -- 办公用品
(229, 2, 21),   -- 消息中心
(230, 2, 22),   -- 文件资料库
(231, 2, 55),   -- 个人信息
(232, 2, 56),   -- 修改密码
(233, 2, 57);   -- 账户管理
