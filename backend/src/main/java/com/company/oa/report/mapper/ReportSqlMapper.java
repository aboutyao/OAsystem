package com.company.oa.report.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportSqlMapper {

    // Workflow Efficiency Report
    @Select("""
            select count(*) from wf_process_instance
            where started_at >= #{from} and started_at < #{to}
            """)
    Long countWfInstancesByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("select count(*) from wf_process_instance where status = #{status} and started_at >= #{from} and started_at < #{to}")
    Long countWfInstancesByStatusAndDateRange(@Param("status") String status,
                                              @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select avg(timestampdiff(SECOND, started_at, ended_at) / 3600.0)
            from wf_process_instance
            where ended_at is not null and started_at >= #{from} and started_at < #{to}
            """)
    Double avgWfProcessHours(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select business_type as businessType, count(*) as count
            from wf_process_instance
            where started_at >= #{from} and started_at < #{to}
            group by business_type
            order by count desc
            """)
    List<Map<String, Object>> groupWfInstancesByBusinessType(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Todo Summary
    @Select("select count(*) from wf_task where status = #{status}")
    Long countWfTasksByStatus(@Param("status") String status);

    @Select("""
            select count(*) from wf_task
            where status = 'PENDING' and due_at is not null and due_at < CURRENT_TIMESTAMP
            """)
    Long countWfTasksOverdue();

    @Select("""
            select wt.assignee_id as assigneeId, max(wt.assignee_name_snapshot) as name, count(*) as count
            from wf_task wt
            where wt.status = 'PENDING'
            group by wt.assignee_id
            order by count desc
            limit 10
            """)
    List<Map<String, Object>> topTodoAssignees();

    // Leave Summary
    @Select("""
            select count(*) from oa_leave
            where deleted = 0 and start_at >= #{from} and start_at < #{to}
            """)
    Long countLeavesByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select count(*) from oa_leave
            where deleted = 0 and status = #{status} and start_at >= #{from} and start_at < #{to}
            """)
    Long countLeavesByStatusAndDateRange(@Param("status") String status,
                                          @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select coalesce(sum(duration_days), 0) from oa_leave
            where deleted = 0 and status = 'APPROVED' and start_at >= #{from} and start_at < #{to}
            """)
    Double sumLeaveDaysApproved(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select leave_type as leaveType, count(*) as count, coalesce(sum(duration_days),0) as totalDays
            from oa_leave
            where deleted = 0 and status = 'APPROVED' and start_at >= #{from} and start_at < #{to}
            group by leave_type
            order by count desc
            """)
    List<Map<String, Object>> groupLeavesByType(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Expense Summary
    @Select("""
            select count(*) from oa_expense
            where deleted = 0 and created_at >= #{from} and created_at < #{to}
            """)
    Long countExpensesByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select coalesce(sum(total_amount), 0) from oa_expense
            where deleted = 0 and status = 'APPROVED' and created_at >= #{from} and created_at < #{to}
            """)
    Double sumExpenseTotalAmountApproved(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select coalesce(sum(paid_amount), 0) from oa_expense
            where deleted = 0 and payment_status = 'PAID' and created_at >= #{from} and created_at < #{to}
            """)
    Double sumExpensePaidAmount(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select i.fee_type as category, count(*) as count, coalesce(sum(i.amount), 0) as amount
            from oa_expense_item i
            join oa_expense e on e.id = i.expense_id and e.deleted = 0
            where e.created_at >= #{from} and e.created_at < #{to}
            group by i.fee_type
            order by amount desc
            """)
    List<Map<String, Object>> groupExpensesByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Contract Summary
    @Select("""
            select count(*) from contract_info
            where deleted = 0 and (sign_date is null or (sign_date >= #{from} and sign_date <= #{to}))
            """)
    Long countContractsByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select coalesce(sum(amount), 0) from contract_info
            where deleted = 0 and (sign_date is null or (sign_date >= #{from} and sign_date <= #{to}))
            """)
    Double sumContractAmounts(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select contract_type as contractType, count(*) as count, coalesce(sum(amount), 0) as amount
            from contract_info
            where deleted = 0 and (sign_date is null or (sign_date >= #{from} and sign_date <= #{to}))
            group by contract_type
            order by count desc
            """)
    List<Map<String, Object>> groupContractsByType(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("""
            select count(*) from contract_info
            where deleted = 0 and end_date is not null
              and end_date >= CURRENT_DATE and end_date <= DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY)
            """)
    Long countContractsExpiringSoon();

    // Asset Summary
    @Select("select count(*) from asset_info where deleted = 0")
    Long countAssets();

    @Select("select count(*) from asset_info where deleted = 0 and status = #{status}")
    Long countAssetsByStatus(@Param("status") String status);

    @Select("select coalesce(sum(purchase_amount), 0) from asset_info where deleted = 0")
    Double sumAssetPurchaseAmounts();

    @Select("""
            select asset_category as category, count(*) as count, coalesce(sum(purchase_amount), 0) as price
            from asset_info where deleted = 0
            group by asset_category
            order by count desc
            """)
    List<Map<String, Object>> groupAssetsByCategory();

    // User Summary
    @Select("""
            select count(*) from org_user
            where deleted = 0 and account_status = 'ENABLED'
            """)
    Long countActiveUsers();

    @Select("select count(*) from org_user where deleted = 0")
    Long countAllUsers();

    @Select("select count(*) from org_dept where deleted = 0 and status = 'ENABLED'")
    Long countActiveDepts();

    @Select("""
            select d.id as deptId, d.dept_name as deptName, count(u.id) as count
            from org_dept d
            left join org_user u on u.main_dept_id = d.id and u.deleted = 0 and u.account_status = 'ENABLED'
            where d.deleted = 0
            group by d.id, d.dept_name
            order by count desc
            """)
    List<Map<String, Object>> groupUsersByDept();
}
