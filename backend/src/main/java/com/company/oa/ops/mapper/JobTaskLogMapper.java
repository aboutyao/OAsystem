package com.company.oa.ops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.ops.JobTaskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface JobTaskLogMapper extends BaseMapper<JobTaskLog> {

    @Select("""
            <script>
            select id, job_code as jobCode, job_name as jobName, status,
                   start_at as startAt, end_at as endAt, duration_ms as durationMs,
                   success_count as successCount, fail_count as failCount,
                   fail_reason as failReason, triggered_by as triggeredBy
            from job_task_log
            <where>
                <if test="jobCode != null and jobCode != ''">and job_code = #{jobCode}</if>
                <if test="status != null and status != ''">and status = #{status}</if>
            </where>
            order by start_at desc, id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectPageByConditions(@Param("jobCode") String jobCode,
                                                      @Param("status") String status,
                                                      @Param("limit") long limit,
                                                      @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from job_task_log
            <where>
                <if test="jobCode != null and jobCode != ''">and job_code = #{jobCode}</if>
                <if test="status != null and status != ''">and status = #{status}</if>
            </where>
            </script>
            """)
    Long countByConditions(@Param("jobCode") String jobCode, @Param("status") String status);
}