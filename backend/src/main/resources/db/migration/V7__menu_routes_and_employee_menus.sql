-- 与 UI_PAGES.md 路由对齐；为 EMPLOYEE 分配最小可访问菜单

UPDATE perm_menu SET route_path = '/oa', menu_name = 'OA 业务', sort_order = 50 WHERE id = 7;

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (12, NULL, 'notices', '通知公告', '/notices', 'common/PlaceholderView', NULL, 5, 1, 'ENABLED'),
    (13, NULL, 'oa_leaves', '请假', '/oa/leaves', 'common/PlaceholderView', NULL, 51, 1, 'ENABLED'),
    (14, NULL, 'oa_expenses', '报销', '/oa/expenses', 'common/PlaceholderView', NULL, 52, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (12, 1, 12),
    (13, 1, 13),
    (14, 1, 14);

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (15, 3, 1),
    (16, 3, 2),
    (17, 3, 12),
    (18, 3, 13),
    (19, 3, 14);

INSERT INTO perm_role_button (id, role_id, button_id) VALUES
    (8, 3, 1),
    (9, 3, 2);
