package com.company.oa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.wf.WfProcessVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface WfProcessVersionMapper extends BaseMapper<WfProcessVersion> {

    @Update("update wf_process_version set status = 'ARCHIVED', updated_at = #{now} where template_id = #{templateId} and status = 'PUBLISHED'")
    int archivePublished(@Param("templateId") long templateId, @Param("now") LocalDateTime now);

    @Update("""
            update wf_process_version set status = 'PUBLISHED', published_at = #{now}, published_by = #{publishedBy},
                   flowable_definition_id = #{flowableDefId}, updated_at = #{now}
            where id = #{versionId}
            """)
    int publish(@Param("versionId") long versionId, @Param("publishedBy") long publishedBy,
                @Param("flowableDefId") String flowableDefId, @Param("now") LocalDateTime now);
}
