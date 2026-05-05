package com.company.oa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.wf.WfProcessTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface WfProcessTemplateMapper extends BaseMapper<WfProcessTemplate> {

    @Select("""
            select t.id as templateId, v.id as versionId, v.flowable_definition_id as procKey
            from wf_process_version v
            join wf_process_template t on t.id = v.template_id
            where v.status = 'PUBLISHED' and t.status = 'ENABLED' and t.business_type = #{businessType}
            order by v.version_no desc
            limit 1
            """)
    Map<String, Object> resolvePublishedVersion(@Param("businessType") String businessType);

    @Select("""
            select t.id as templateId, v.id as versionId, v.flowable_definition_id as procKey
            from wf_process_version v
            join wf_process_template t on t.id = v.template_id
            where v.status = 'PUBLISHED' and t.status = 'ENABLED' and t.business_type = 'GENERIC'
            order by v.version_no desc
            limit 1
            """)
    Map<String, Object> resolveGenericPublishedVersion();
}
