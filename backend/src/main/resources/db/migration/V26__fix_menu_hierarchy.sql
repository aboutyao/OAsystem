-- V26: Fix menu hierarchy
-- Problem: 24 menus all have parent_id=NULL, making the sidebar a flat list.
-- Solution:
--   1. Clear route_path from group-parent menus so the frontend treats them as expandable groups
--   2. Re-parent OA business leaf menus under their correct group (id=7)
--   3. Create a "个人设置" group for account-related menus (id=55,56)

-- ============================================================
-- 1. Turn group parents into proper groups (remove route_path)
--    These menus have children and should NOT navigate to a page;
--    they should only expand/collapse their child list.
-- ============================================================
UPDATE perm_menu SET route_path = NULL WHERE id IN (3, 4, 5, 6, 7, 8, 9, 10, 11, 23);

-- ============================================================
-- 2. Move OA leaf menus under OA 业务 (id=7)
--    These were originally inserted with parent_id=NULL but are
--    logically children of the "OA 业务" group.
-- ============================================================
UPDATE perm_menu SET parent_id = 7 WHERE id IN (13, 14, 15, 16, 18, 19, 20, 22);

-- Re-sort the newly-adopted children so primary operations come first
-- (before the report/admin items that were already children of id=7).
UPDATE perm_menu SET sort_order = 1  WHERE id = 13; -- 请假
UPDATE perm_menu SET sort_order = 2  WHERE id = 14; -- 报销
UPDATE perm_menu SET sort_order = 3  WHERE id = 15; -- 用章申请
UPDATE perm_menu SET sort_order = 4  WHERE id = 16; -- 采购申请
UPDATE perm_menu SET sort_order = 5  WHERE id = 18; -- 会议室
UPDATE perm_menu SET sort_order = 6  WHERE id = 19; -- 固定资产
UPDATE perm_menu SET sort_order = 7  WHERE id = 20; -- 办公用品
UPDATE perm_menu SET sort_order = 8  WHERE id = 22; -- 文件资料库

-- Bump the existing report/admin children so they appear after the primary items.
UPDATE perm_menu SET sort_order = 9  WHERE id = 35; -- 请假报表
UPDATE perm_menu SET sort_order = 10 WHERE id = 36; -- 财务审核
UPDATE perm_menu SET sort_order = 11 WHERE id = 37; -- 报销报表
UPDATE perm_menu SET sort_order = 12 WHERE id = 48; -- 用章台账
UPDATE perm_menu SET sort_order = 13 WHERE id = 49; -- 印章归还
UPDATE perm_menu SET sort_order = 14 WHERE id = 50; -- 到货登记
UPDATE perm_menu SET sort_order = 15 WHERE id = 51; -- 验收管理

-- ============================================================
-- 3. Create "个人设置" group and adopt account menus
-- ============================================================
INSERT INTO perm_menu (id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status)
VALUES (57, NULL, 'personal', '个人设置', NULL, NULL, 'Setting', 100, 1, 'ENABLED');

UPDATE perm_menu SET parent_id = 57, sort_order = 1 WHERE id = 55; -- 个人信息
UPDATE perm_menu SET parent_id = 57, sort_order = 2 WHERE id = 56; -- 修改密码

-- Grant the new group menu to all roles that already had 55 or 56
INSERT INTO perm_role_menu (id, role_id, menu_id)
SELECT 128 + role_id, role_id, 57
FROM perm_role_menu
WHERE menu_id = 55
  AND role_id NOT IN (SELECT role_id FROM perm_role_menu WHERE menu_id = 57);
