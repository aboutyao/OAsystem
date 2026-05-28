package com.company.oa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.wf.WfTaskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WfTaskRecordMapper extends BaseMapper<WfTaskRecord> {

    @Select("""
            select action, operator_name_snapshot as operatorName, node_name as nodeName, comment, operated_at as operatedAt
            from wf_task_record
            where wf_instance_id = #{wfInstanceId}
            order by operated_at, id
            """)
    List<Map<String, Object>> selectTimeline(@Param("wfInstanceId") long wfInstanceId);

    @Select("select count(*) from wf_task_record where wf_instance_id = #{wfInstanceId} and action = #{action}")
    Long countByInstanceIdAndAction(@Param("wfInstanceId") long wfInstanceId, @Param("action") String action);

    @Select("""
            select id, wf_instance_id as wfInstanceId, task_id as taskId, action, operator_id as operatorId,
                   operator_name_snapshot as operatorName, node_name as nodeName, comment,
                   attachment_ids as attachmentIds, parent_record_id as parentRecordId, operated_at as operatedAt
            from wf_task_record
            where id = #{recordId} and wf_instance_id = #{wfInstanceId}
            """)
    Map<String, Object> selectRecordById(@Param("wfInstanceId") long wfInstanceId, @Param("recordId") long recordId);

    @Select("""
            select id, wf_instance_id as wfInstanceId, task_id as taskId, action, operator_id as operatorId,
                   operator_name_snapshot as operatorName, node_name as nodeName, comment,
                   attachment_ids as attachmentIds, parent_record_id as parentRecordId, operated_at as operatedAt
            from wf_task_record
            where parent_record_id = #{parentRecordId}
            order by operated_at, id
            """)
    List<Map<String, Object>> selectRepliesByParentId(@Param("parentRecordId") long parentRecordId);

    @Select("""
            select count(*) from wf_task_record
            where parent_record_id = #{parentRecordId}
            """)
    Long countRepliesByParentId(@Param("parentRecordId") long parentRecordId);

    /**
     * Return approval performance statistics for a user.
     * Calculates average response hours (time between task creation and approval action)
     * and total approval count from wf_task_record joined with wf_task.
     * Used by the smart routing engine to score potential approvers.
     */
    @Select("""
            select count(*) as totalApprovals,
                   avg(TIMESTAMPDIFF(HOUR, t.created_at, r.operated_at)) as avgResponseHours
            from wf_task_record r
            join wf_task t on t.id = r.task_id
            where r.operator_id = #{userId}
              and r.action in ('APPROVED', 'REJECTED')
              and r.operated_at is not null
              and t.created_at is not null
            """)
    Map<String, Object> selectApprovalStats(@Param("userId") long userId);
}
