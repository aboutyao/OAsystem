package com.company.oa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.system.SysExportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysExportTaskMapper extends BaseMapper<SysExportTask> {

    @Select("""
            select t.id, t.task_code as taskCode, t.business_type as businessType, t.file_name as fileName,
                   t.file_size as fileSize, t.row_count as rowCount, t.status,
                   t.error_summary as errorSummary, t.submitted_by as submittedBy,
                   u.real_name as submittedByName, t.submitted_at as submittedAt,
                   t.finished_at as finishedAt, t.download_count as downloadCount
            from sys_export_task t
            left join org_user u on u.id = t.submitted_by
            where t.id = #{id}
            """)
    List<Map<String, Object>> selectExportTaskById(@Param("id") long id);

    @Select("""
            <script>
            select t.id, t.task_code as taskCode, t.business_type as businessType, t.file_name as fileName,
                   t.file_size as fileSize, t.row_count as rowCount, t.status,
                   t.error_summary as errorSummary, t.submitted_by as submittedBy,
                   u.real_name as submittedByName, t.submitted_at as submittedAt,
                   t.finished_at as finishedAt, t.download_count as downloadCount
            from sys_export_task t
            left join org_user u on u.id = t.submitted_by
            where 1=1
            <if test='businessType != null and businessType != ""'>and t.business_type = #{businessType}</if>
            order by t.submitted_at desc, t.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectExportTasks(@Param("businessType") String businessType,
                                                 @Param("limit") long limit,
                                                 @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from sys_export_task where 1=1
            <if test='businessType != null and businessType != ""'>and business_type = #{businessType}</if>
            </script>
            """)
    Long countExportTasks(@Param("businessType") String businessType);
}
