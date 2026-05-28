package com.company.oa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.wf.WfProcessInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface WfProcessInstanceMapper extends BaseMapper<WfProcessInstance> {

    @Select("""
            select id as wfInstanceId, process_instance_id as processInstanceId, template_id as templateId,
                   process_version_id as processVersionId, business_type as businessType, business_id as businessId,
                   title, starter_id as starterId, starter_name_snapshot as starterName, current_node_name as currentNodeName,
                   status, started_at as startedAt, ended_at as endedAt,
                   sla_deadline as slaDeadline, sla_breached as slaBreached
            from wf_process_instance where id = #{id}
            """)
    Map<String, Object> loadInstance(@Param("id") long id);

    @Select("""
            select inst.id as wfInstanceId, inst.process_instance_id as processInstanceId,
                   inst.business_type as businessType, inst.business_id as businessId, inst.title,
                   inst.starter_id as starterId, inst.starter_name_snapshot as starterName,
                   inst.current_node_name as currentNodeName, inst.status,
                   inst.started_at as startedAt, inst.ended_at as endedAt,
                   case when inst.status = 'APPROVING' then 'NO_PENDING_TASK' else 'ENDED_WITHOUT_TIME' end as reason
            from wf_process_instance inst
            where (inst.status = 'APPROVING' and not exists (
                    select 1 from wf_task wt where wt.wf_instance_id = inst.id and wt.status = 'PENDING'
                  ))
               or (inst.status in ('APPROVED','REJECTED','WITHDRAWN','TERMINATED') and inst.ended_at is null)
            order by inst.started_at desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectExceptions(@Param("limit") long limit, @Param("offset") long offset);

    @Select("""
            select count(*) from wf_process_instance inst
            where (inst.status = 'APPROVING' and not exists (
                    select 1 from wf_task wt where wt.wf_instance_id = inst.id and wt.status = 'PENDING'
                  ))
               or (inst.status in ('APPROVED','REJECTED','WITHDRAWN','TERMINATED') and inst.ended_at is null)
            """)
    Long countExceptions();

    @Update("""
            update wf_process_instance set status = #{status}, current_node_name = #{nodeName}, ended_at = #{endedAt}
            where id = #{id}
            """)
    int updateStatus(@Param("id") long id, @Param("status") String status,
                     @Param("nodeName") String nodeName, @Param("endedAt") LocalDateTime endedAt);

    @Update("update wf_process_instance set current_node_name = #{nodeName} where id = #{id}")
    int updateCurrentNode(@Param("id") long id, @Param("nodeName") String nodeName);

    @Select("""
            select inst.id as wfInstanceId, inst.title, inst.business_type as businessType,
                   inst.business_id as businessId, inst.status, inst.current_node_name as currentNodeName,
                   inst.started_at as startedAt
            from wf_process_instance inst
            where inst.starter_id = #{starterId}
            order by inst.started_at desc, inst.id desc
            limit #{limit}
            """)
    List<Map<String, Object>> selectMyStarted(@Param("starterId") long starterId, @Param("limit") int limit);

    @Select("select count(*) from wf_process_instance where starter_id = #{starterId} and status = 'APPROVING'")
    Long countStartedByStarter(@Param("starterId") long starterId);

    @Update("update wf_process_instance set sla_deadline = #{slaDeadline} where id = #{id}")
    int updateSlaDeadline(@Param("id") long id, @Param("slaDeadline") LocalDateTime slaDeadline);

    @Update("update wf_process_instance set sla_breached = 1 where id = #{id} and sla_breached = 0")
    int markSlaBreach(@Param("id") long id);

    @Select("""
            select count(*) from wf_process_instance inst
            where inst.starter_id = #{starterId}
              and ((inst.status = 'APPROVING' and not exists (
                      select 1 from wf_task wt where wt.wf_instance_id = inst.id and wt.status = 'PENDING'
                    ))
                   or (inst.status in ('APPROVED','REJECTED','WITHDRAWN','TERMINATED') and inst.ended_at is null))
            """)
    Long countExceptionByStarter(@Param("starterId") long starterId);

    @Select("""
            select inst.id as wfInstanceId, inst.title, inst.sla_deadline as slaDeadline
            from wf_process_instance inst
            where inst.status = 'APPROVING' and inst.sla_breached = 0
              and inst.sla_deadline is not null
              and inst.sla_deadline between #{now} and #{deadline}
            order by inst.sla_deadline asc
            """)
    List<Map<String, Object>> selectSlaBreachesAtRisk(@Param("starterId") long starterId,
                                                       @Param("now") LocalDateTime now,
                                                       @Param("deadline") LocalDateTime deadline);

    @Select("""
            select inst.id as wfInstanceId, inst.title, inst.sla_deadline as slaDeadline
            from wf_process_instance inst
            where inst.status = 'APPROVING' and inst.sla_breached = 1
            order by inst.sla_deadline asc
            """)
    List<Map<String, Object>> selectSlaBreachedInstances(@Param("starterId") long starterId);
}
