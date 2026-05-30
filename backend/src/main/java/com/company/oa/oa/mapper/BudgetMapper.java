package com.company.oa.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.oa.Budget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BudgetMapper extends BaseMapper<Budget> {

    @Select("SELECT b.*, d.dept_name as deptName " +
            "FROM oa_budget b " +
            "LEFT JOIN org_dept d ON b.dept_id = d.id " +
            "WHERE b.status = 'ACTIVE' AND b.deleted = 0 " +
            "AND (b.used_amount / b.budget_amount) >= (b.warning_threshold / 100) " +
            "ORDER BY (b.used_amount / b.budget_amount) DESC")
    List<Map<String, Object>> selectBudgetsNearLimit();

    @Select("SELECT b.*, d.dept_name as deptName " +
            "FROM oa_budget b " +
            "LEFT JOIN org_dept d ON b.dept_id = d.id " +
            "WHERE b.status = 'ACTIVE' AND b.deleted = 0 " +
            "AND b.used_amount > b.budget_amount " +
            "ORDER BY (b.used_amount - b.budget_amount) DESC")
    List<Map<String, Object>> selectOverBudgets();
}
