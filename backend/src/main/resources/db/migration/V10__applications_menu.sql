-- 与 UI_PAGES.md「我发起的」「抄送我的」路由对齐；员工可访问 /applications 及 /applications/cc

INSERT INTO perm_menu (
    id, parent_id, menu_code, menu_name, route_path, component, icon, sort_order, visible, status
) VALUES
    (17, NULL, 'applications', '我的申请', '/applications', 'common/PlaceholderView', NULL, 3, 1, 'ENABLED');

INSERT INTO perm_role_menu (id, role_id, menu_id) VALUES
    (24, 1, 17),
    (25, 3, 17);
