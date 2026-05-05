package com.company.oa.ops.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.ops.BackupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BackupRecordMapper extends BaseMapper<BackupRecord> {

    @Select("""
            <script>
            select id, backup_type as backupType, backup_path as backupPath, backup_size as backupSize,
                   status, started_at as startedAt, finished_at as finishedAt, duration_ms as durationMs,
                   fail_reason as failReason, triggered_by as triggeredBy
            from backup_record
            <where>
                <if test="backupType != null and backupType != ''">and backup_type = #{backupType}</if>
                <if test="status != null and status != ''">and status = #{status}</if>
            </where>
            order by started_at desc, id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectPageByConditions(@Param("backupType") String backupType,
                                                      @Param("status") String status,
                                                      @Param("limit") long limit,
                                                      @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from backup_record
            <where>
                <if test="backupType != null and backupType != ''">and backup_type = #{backupType}</if>
                <if test="status != null and status != ''">and status = #{status}</if>
            </where>
            </script>
            """)
    Long countByConditions(@Param("backupType") String backupType, @Param("status") String status);
}