package com.company.oa.knowledge;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 知识图谱服务
 * 自动关联：供应商→历史合同→相关采购单→负责人
 */
@Service
public class KnowledgeGraphService {
    private final JdbcTemplate jdbcTemplate;

    public KnowledgeGraphService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取实体关联关系
     */
    public Map<String, Object> getEntityRelations(String entityType, Long entityId) {
        Map<String, Object> result = new HashMap<>();
        result.put("entityType", entityType);
        result.put("entityId", entityId);
        result.put("relations", findRelatedEntities(entityType, entityId));
        return result;
    }

    /**
     * 查找关联实体
     */
    private List<Map<String, Object>> findRelatedEntities(String entityType, Long entityId) {
        List<Map<String, Object>> relations = new ArrayList<>();

        switch (entityType) {
            case "SUPPLIER":
                relations.addAll(findSupplierRelations(entityId));
                break;
            case "CONTRACT":
                relations.addAll(findContractRelations(entityId));
                break;
            case "USER":
                relations.addAll(findUserRelations(entityId));
                break;
            case "DEPARTMENT":
                relations.addAll(findDeptRelations(entityId));
                break;
        }

        return relations;
    }

    private List<Map<String, Object>> findSupplierRelations(Long supplierId) {
        List<Map<String, Object>> relations = new ArrayList<>();

        // 查找该供应商的相关采购单
        List<Map<String, Object>> purchases = jdbcTemplate.queryForList(
            "SELECT id, total_amount, status, created_at FROM oa_purchase WHERE supplier_id = ? ORDER BY created_at DESC LIMIT 10",
            supplierId
        );
        for (Map<String, Object> purchase : purchases) {
            Map<String, Object> relation = new HashMap<>();
            relation.put("type", "PURCHASE");
            relation.put("id", purchase.get("id"));
            relation.put("label", "采购单 #" + purchase.get("id"));
            relation.put("amount", purchase.get("total_amount"));
            relation.put("status", purchase.get("status"));
            relations.add(relation);
        }

        // 查找该供应商的相关合同
        List<Map<String, Object>> contracts = jdbcTemplate.queryForList(
            "SELECT id, contract_name, status, end_date FROM contract_info WHERE supplier_id = ? ORDER BY created_at DESC LIMIT 5",
            supplierId
        );
        for (Map<String, Object> contract : contracts) {
            Map<String, Object> relation = new HashMap<>();
            relation.put("type", "CONTRACT");
            relation.put("id", contract.get("id"));
            relation.put("label", contract.get("contract_name"));
            relation.put("status", contract.get("status"));
            relation.put("endDate", contract.get("end_date"));
            relations.add(relation);
        }

        return relations;
    }

    private List<Map<String, Object>> findContractRelations(Long contractId) {
        List<Map<String, Object>> relations = new ArrayList<>();

        // 查找合同相关的采购单
        List<Map<String, Object>> purchases = jdbcTemplate.queryForList(
            "SELECT id, total_amount, status FROM oa_purchase WHERE contract_id = ?",
            contractId
        );
        for (Map<String, Object> purchase : purchases) {
            Map<String, Object> relation = new HashMap<>();
            relation.put("type", "PURCHASE");
            relation.put("id", purchase.get("id"));
            relation.put("label", "采购单 #" + purchase.get("id"));
            relations.add(relation);
        }

        // 查找合同相关的负责人
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT DISTINCT u.id, u.real_name FROM org_user u JOIN oa_purchase p ON u.id = p.created_by WHERE p.contract_id = ?",
            contractId
        );
        for (Map<String, Object> user : users) {
            Map<String, Object> relation = new HashMap<>();
            relation.put("type", "USER");
            relation.put("id", user.get("id"));
            relation.put("label", user.get("real_name"));
            relations.add(relation);
        }

        return relations;
    }

    private List<Map<String, Object>> findUserRelations(Long userId) {
        List<Map<String, Object>> relations = new ArrayList<>();

        // 查找用户发起的采购单
        List<Map<String, Object>> purchases = jdbcTemplate.queryForList(
            "SELECT id, total_amount, status, supplier_name FROM oa_purchase WHERE created_by = ? ORDER BY created_at DESC LIMIT 10",
            userId
        );
        for (Map<String, Object> purchase : purchases) {
            Map<String, Object> relation = new HashMap<>();
            relation.put("type", "PURCHASE");
            relation.put("id", purchase.get("id"));
            relation.put("label", "采购单 #" + purchase.get("id"));
            relation.put("supplier", purchase.get("supplier_name"));
            relations.add(relation);
        }

        // 查找用户发起的报销
        List<Map<String, Object>> expenses = jdbcTemplate.queryForList(
            "SELECT id, total_amount, status FROM oa_expense WHERE created_by = ? ORDER BY created_at DESC LIMIT 10",
            userId
        );
        for (Map<String, Object> expense : expenses) {
            Map<String, Object> relation = new HashMap<>();
            relation.put("type", "EXPENSE");
            relation.put("id", expense.get("id"));
            relation.put("label", "报销单 #" + expense.get("id"));
            relation.put("amount", expense.get("total_amount"));
            relations.add(relation);
        }

        return relations;
    }

    private List<Map<String, Object>> findDeptRelations(Long deptId) {
        List<Map<String, Object>> relations = new ArrayList<>();

        // 查找部门下的用户
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT id, real_name FROM org_user WHERE main_dept_id = ? LIMIT 20",
            deptId
        );
        for (Map<String, Object> user : users) {
            Map<String, Object> relation = new HashMap<>();
            relation.put("type", "USER");
            relation.put("id", user.get("id"));
            relation.put("label", user.get("real_name"));
            relations.add(relation);
        }

        // 查找部门下的子部门
        List<Map<String, Object>> childDepts = jdbcTemplate.queryForList(
            "SELECT id, name FROM org_department WHERE parent_id = ? AND deleted = 0",
            deptId
        );
        for (Map<String, Object> dept : childDepts) {
            Map<String, Object> relation = new HashMap<>();
            relation.put("type", "DEPARTMENT");
            relation.put("id", dept.get("id"));
            relation.put("label", dept.get("name"));
            relations.add(relation);
        }

        return relations;
    }

    /**
     * 搜索实体
     */
    public List<Map<String, Object>> searchEntities(String keyword, String entityType) {
        List<Map<String, Object>> results = new ArrayList<>();

        String searchPattern = "%" + keyword + "%";

        if (entityType == null || "USER".equals(entityType)) {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id, real_name as name, 'USER' as type FROM org_user WHERE real_name LIKE ? OR username LIKE ? LIMIT 10",
                searchPattern, searchPattern
            );
            results.addAll(users);
        }

        if (entityType == null || "SUPPLIER".equals(entityType)) {
            List<Map<String, Object>> suppliers = jdbcTemplate.queryForList(
                "SELECT DISTINCT supplier_name as name, 'SUPPLIER' as type FROM oa_purchase WHERE supplier_name LIKE ? LIMIT 10",
                searchPattern
            );
            results.addAll(suppliers);
        }

        if (entityType == null || "CONTRACT".equals(entityType)) {
            List<Map<String, Object>> contracts = jdbcTemplate.queryForList(
                "SELECT id, contract_name as name, 'CONTRACT' as type FROM contract_info WHERE contract_name LIKE ? LIMIT 10",
                searchPattern
            );
            results.addAll(contracts);
        }

        return results;
    }
}
