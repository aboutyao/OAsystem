package com.company.oa.permission;

import com.company.oa.common.service.SequenceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 权限继承服务
 * 部门权限自动继承，减少配置工作
 */
@Service
public class PermissionInheritanceService {
    private final JdbcTemplate jdbcTemplate;
    private final SequenceService sequenceService;

    public PermissionInheritanceService(JdbcTemplate jdbcTemplate, SequenceService sequenceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sequenceService = sequenceService;
    }

    /**
     * 获取用户的有效权限（包含继承的）
     */
    public Set<String> getEffectivePermissions(long userId) {
        Set<String> permissions = new HashSet<>();

        // 1. 获取用户直接权限
        List<Map<String, Object>> directPermissions = jdbcTemplate.queryForList(
            "SELECT DISTINCT p.permission_code FROM perm_user_role ur " +
            "JOIN perm_role_menu rm ON ur.role_id = rm.role_id " +
            "JOIN perm_menu p ON rm.menu_id = p.id " +
            "WHERE ur.user_id = ? AND p.status = 'ENABLED'",
            userId
        );
        for (Map<String, Object> perm : directPermissions) {
            permissions.add((String) perm.get("permission_code"));
        }

        // 2. 获取用户所在部门的权限（继承）
        Long deptId = jdbcTemplate.queryForObject(
            "SELECT main_dept_id FROM org_user WHERE id = ?", Long.class, userId
        );

        if (deptId != null) {
            List<Map<String, Object>> deptPermissions = getDeptPermissions(deptId);
            for (Map<String, Object> perm : deptPermissions) {
                permissions.add((String) perm.get("permission_code"));
            }
        }

        return permissions;
    }

    /**
     * 获取部门权限（包含继承的）
     */
    public List<Map<String, Object>> getDeptPermissions(long deptId) {
        List<Map<String, Object>> allPermissions = new ArrayList<>();
        Set<Long> visitedDepts = new HashSet<>();

        // 递归获取部门权限
        collectDeptPermissions(deptId, allPermissions, visitedDepts);

        return allPermissions;
    }

    private void collectDeptPermissions(long deptId, List<Map<String, Object>> permissions, Set<Long> visited) {
        if (visited.contains(deptId)) {
            return; // 防止循环引用
        }
        visited.add(deptId);

        // 获取当前部门的权限
        List<Map<String, Object>> deptPerms = jdbcTemplate.queryForList(
            "SELECT DISTINCT p.permission_code FROM perm_dept_role dr " +
            "JOIN perm_role_menu rm ON dr.role_id = rm.role_id " +
            "JOIN perm_menu p ON rm.menu_id = p.id " +
            "WHERE dr.dept_id = ? AND p.status = 'ENABLED'",
            deptId
        );
        permissions.addAll(deptPerms);

        // 获取父部门权限（继承）
        Long parentDeptId = jdbcTemplate.queryForObject(
            "SELECT parent_id FROM org_department WHERE id = ?", Long.class, deptId
        );

        if (parentDeptId != null && parentDeptId > 0) {
            collectDeptPermissions(parentDeptId, permissions, visited);
        }
    }

    /**
     * 设置部门权限
     */
    @Transactional
    public void setDeptPermissions(long deptId, List<Long> roleIds) {
        // 删除现有权限
        jdbcTemplate.update("DELETE FROM perm_dept_role WHERE dept_id = ?", deptId);

        // 添加新权限
        for (Long roleId : roleIds) {
            long id = sequenceService.nextId("perm_dept_role");
            jdbcTemplate.update(
                "INSERT INTO perm_dept_role (id, dept_id, role_id, created_at) VALUES (?, ?, ?, NOW())",
                id, deptId, roleId
            );
        }
    }

    /**
     * 检查用户是否有某个权限
     */
    public boolean hasPermission(long userId, String permissionCode) {
        Set<String> permissions = getEffectivePermissions(userId);
        return permissions.contains(permissionCode) || permissions.contains("*");
    }

    /**
     * 获取权限继承树
     */
    public Map<String, Object> getPermissionTree(long deptId) {
        Map<String, Object> tree = new HashMap<>();
        tree.put("deptId", deptId);

        // 获取部门信息
        Map<String, Object> dept = jdbcTemplate.queryForMap(
            "SELECT id, name, parent_id FROM org_department WHERE id = ?", deptId
        );
        tree.put("deptName", dept.get("name"));
        tree.put("parentId", dept.get("parent_id"));

        // 获取部门权限
        tree.put("permissions", getDeptPermissions(deptId));

        // 获取子部门
        List<Map<String, Object>> children = jdbcTemplate.queryForList(
            "SELECT id, name FROM org_department WHERE parent_id = ? AND deleted = 0", deptId
        );
        tree.put("children", children);

        return tree;
    }
}
