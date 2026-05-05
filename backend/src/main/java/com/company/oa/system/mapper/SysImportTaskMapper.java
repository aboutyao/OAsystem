package com.company.oa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.system.SysImportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysImportTaskMapper extends BaseMapper<SysImportTask> {

    @Select("""
            select id, task_code as taskCode, business_type as businessType, file_name as fileName,
                   file_size as fileSize, total_rows as totalRows, success_rows as successRows,
                   failed_rows as failedRows, status, error_summary as errorSummary,
                   submitted_by as submittedBy, submitted_at as submittedAt, finished_at as finishedAt
            from sys_import_task where id = #{id}
            """)
    List<Map<String, Object>> selectImportTaskById(@Param("id") long id);

    @Select("""
            <script>
            select t.id, t.task_code as taskCode, t.business_type as businessType, t.file_name as fileName,
                   t.file_size as fileSize, t.total_rows as totalRows, t.success_rows as successRows,
                   t.failed_rows as failedRows, t.status, t.error_summary as errorSummary,
                   t.submitted_by as submittedBy, u.real_name as submittedByName,
                   t.submitted_at as submittedAt, t.finished_at as finishedAt
            from sys_import_task t
            left join org_user u on u.id = t.submitted_by
            where 1=1
            <if test='businessType != null and businessType != ""'>and t.business_type = #{businessType}</if>
            order by t.submitted_at desc, t.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectImportTasks(@Param("businessType") String businessType,
                                                 @Param("limit") long limit,
                                                 @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from sys_import_task where 1=1
            <if test='businessType != null and businessType != ""'>and business_type = #{businessType}</if>
            </script>
            """)
    Long countImportTasks(@Param("businessType") String businessType);
}
