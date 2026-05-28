package com.company.oa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.wf.WfTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface WfTaskMapper extends BaseMapper<WfTask> {

    @Select("""
            select wt.id, wt.id as taskId, inst.title, wt.node_name as nodeName, wt.status, wt.wf_instance_id as wfInstanceId,
                   inst.process_instance_id as processInstanceId, wt.created_at as createdAt,
                   inst.business_type as businessType, inst.starter_name_snapshot as starterName,
                   TIMESTAMPDIFF(HOUR, wt.created_at, NOW()) as waitHours
            from wf_task wt
            join wf_process_instance inst on inst.id = wt.wf_instance_id
            where wt.assignee_id = #{assigneeId} and wt.status = #{status}
            order by TIMESTAMPDIFF(HOUR, wt.created_at, NOW()) desc, wt.id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectTodoTasks(@Param("assigneeId") long assigneeId,
                                               @Param("status") String status,
                                               @Param("limit") long limit,
                                               @Param("offset") long offset);

    @Select("select count(*) from wf_task wt where wt.assignee_id = #{assigneeId} and wt.status = #{status}")
    Long countTodoTasks(@Param("assigneeId") long assigneeId, @Param("status") String status);

    @Select("""
            select wt.id, wt.id as taskId, inst.title, wt.node_name as nodeName, wt.status, wt.wf_instance_id as wfInstanceId,
                   inst.process_instance_id as processInstanceId, wt.completed_at as completedAt
            from wf_task wt
            join wf_process_instance inst on inst.id = wt.wf_instance_id
            where wt.assignee_id = #{assigneeId} and wt.status in ('COMPLETED','CANCELLED')
            order by coalesce(wt.completed_at, wt.created_at) desc, wt.id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectDoneTasks(@Param("assigneeId") long assigneeId,
                                               @Param("limit") long limit,
                                               @Param("offset") long offset);

    @Select("select count(*) from wf_task wt where wt.assignee_id = #{assigneeId} and wt.status in ('COMPLETED','CANCELLED')")
    Long countDoneTasks(@Param("assigneeId") long assigneeId);

    @Select("""
            select wt.id, wt.flowable_task_id as flowableTaskId, wt.process_instance_id as processInstanceId,
                   wt.wf_instance_id as wfInstanceId, wt.node_id as nodeId, wt.node_name as nodeName,
                   wt.status, wt.assignee_id as assigneeId,
                   wt.add_sign_origin_task_id as addSignOriginTaskId, wt.add_sign_mode as addSignMode
            from wf_task wt where wt.id = #{id}
            """)
    Map<String, Object> loadWfTask(@Param("id") long id);

    @Select("select assignee_id from wf_task where wf_instance_id = #{wfInstanceId} and status = #{status}")
    List<Long> selectAssigneeIdsByInstanceAndStatus(@Param("wfInstanceId") long wfInstanceId,
                                                     @Param("status") String status);

    @Update("""
            update wf_task set status = #{status}, completed_at = #{completedAt}
            where wf_instance_id = #{wfInstanceId} and status = #{oldStatus}
            """)
    int updateStatusByInstanceAndOldStatus(@Param("wfInstanceId") long wfInstanceId,
                                            @Param("status") String status,
                                            @Param("completedAt") LocalDateTime completedAt,
                                            @Param("oldStatus") String oldStatus);

    @Update("update wf_task set status = #{status}, completed_at = #{completedAt} where id = #{id}")
    int updateStatusById(@Param("id") long id, @Param("status") String status,
                          @Param("completedAt") LocalDateTime completedAt);

    @Update("update wf_task set status = #{status} where id = #{id} and status = #{oldStatus}")
    int updateStatusByIdAndOldStatus(@Param("id") long id, @Param("status") String status,
                                      @Param("oldStatus") String oldStatus);

    @Update("""
            update wf_task set assignee_id = #{assigneeId}, assignee_name_snapshot = #{assigneeName},
                   assignee_dept_id = #{assigneeDeptId}
            where id = #{id}
            """)
    int updateAssignee(@Param("id") long id, @Param("assigneeId") long assigneeId,
                        @Param("assigneeName") String assigneeName, @Param("assigneeDeptId") Long assigneeDeptId);

    @Update("""
            update wf_task set status = 'CANCELLED', completed_at = #{now}
            where flowable_task_id = #{flowableTaskId} and id <> #{excludeId} and status = 'PENDING'
              and (add_sign_mode = 'PARALLEL' or add_sign_origin_task_id is null)
            """)
    int cancelParallelSiblings(@Param("flowableTaskId") String flowableTaskId,
                                @Param("excludeId") long excludeId,
                                @Param("now") LocalDateTime now);

    @Update("delete from wf_task where wf_instance_id = #{wfInstanceId} and status = #{status} and add_sign_mode is null and add_sign_origin_task_id is null")
    int deletePlainPendingByInstance(@Param("wfInstanceId") long wfInstanceId, @Param("status") String status);

    @Select("""
            select wt.id as taskId, inst.title, wt.node_name as nodeName, wt.status,
                   wt.wf_instance_id as wfInstanceId, inst.business_type as businessType,
                   inst.business_id as businessId, wt.created_at as createdAt
            from wf_task wt
            join wf_process_instance inst on inst.id = wt.wf_instance_id
            where wt.assignee_id = #{assigneeId} and wt.status = 'PENDING'
            order by wt.created_at desc, wt.id desc
            limit #{limit}
            """)
    List<Map<String, Object>> selectDashboardTodos(@Param("assigneeId") long assigneeId, @Param("limit") int limit);

    @Select("""
            select AVG(TIMESTAMPDIFF(HOUR, wt.created_at, wt.completed_at)) as avgHours
            from wf_task wt
            where wt.assignee_id = #{userId} and wt.status in ('COMPLETED','CANCELLED')
              and wt.completed_at is not null
            """)
    Map<String, Object> selectApprovalTimeStats(@Param("userId") long userId);

    @Select("""
            select AVG(TIMESTAMPDIFF(HOUR, wt.created_at, wt.completed_at)) as avgHours
            from wf_task wt
            where wt.assignee_id <> #{userId} and wt.status in ('COMPLETED','CANCELLED')
              and wt.completed_at is not null
            """)
    Map<String, Object> selectTeamApprovalTimeStats(@Param("userId") long userId);

    // ─── Predictive Insights: Approval Workload ─────────────────────────

    /**
     * Count tasks completed by a user per day over the last N days (for workload averaging).
     * Returns list of {day, taskCount} for each calendar day in the window.
     */
    @Select("""
            select DATE(wt.completed_at) as day, count(*) as taskCount
            from wf_task wt
            where wt.assignee_id = #{userId}
              and wt.status in ('COMPLETED', 'CANCELLED')
              and wt.completed_at is not null
              and wt.completed_at >= #{since}
            group by DATE(wt.completed_at)
            order by day asc
            """)
    List<Map<String, Object>> selectDailyCompletedTaskCounts(@Param("userId") long userId,
                                                             @Param("since") LocalDateTime since);

    /**
     * SLA tasks for a user: tasks approaching SLA deadline (< 4h remaining) or already breached.
     * Returns tasks where sla_deadline is set and status is still PENDING.
     */
    @Select("""
            select wt.id as taskId, wt.sla_deadline as slaDeadline, wt.node_name as nodeName,
                   wt.created_at as createdAt, inst.title as title, inst.business_type as businessType,
                   TIMESTAMPDIFF(HOUR, NOW(), wt.sla_deadline) as hoursRemaining
            from wf_task wt
            join wf_process_instance inst on inst.id = wt.wf_instance_id
            where wt.assignee_id = #{userId} and wt.status = 'PENDING'
              and wt.sla_deadline is not null
            order by wt.sla_deadline asc
            """)
    List<Map<String, Object>> selectSlaTasksForUser(@Param("userId") long userId);
}
