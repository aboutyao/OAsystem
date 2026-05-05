package com.company.oa.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.form.FormTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FormTemplateMapper extends BaseMapper<FormTemplate> {

    @Select("""
            select id, template_code as templateCode, template_name as templateName,
                   business_type as businessType, description, status,
                   current_version_id as currentVersionId,
                   created_at as createdAt, updated_at as updatedAt
            from form_template where id = #{id} and deleted = 0
            """)
    List<Map<String, Object>> selectTemplateById(@Param("id") long id);

    @Select("""
            select id, template_code as templateCode, template_name as templateName,
                   business_type as businessType, description, status,
                   current_version_id as currentVersionId,
                   created_at as createdAt, updated_at as updatedAt
            from form_template where deleted = 0
            order by id
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectTemplates(@Param("limit") long limit, @Param("offset") long offset);

    @Select("select count(*) from form_template where deleted = 0")
    Long countTemplates();

    @Select("""
            select t.id as templateId, t.template_code as templateCode, t.template_name as templateName,
                   v.id as versionId, v.version_no as versionNo, v.fields_json as fieldsJson,
                   v.layout_json as layoutJson
            from form_template t
            join form_version v on v.id = t.current_version_id
            where t.business_type = #{businessType} and t.deleted = 0 and t.status = 'PUBLISHED'
            order by t.id desc
            limit 1
            """)
    List<Map<String, Object>> selectRuntime(@Param("businessType") String businessType);
}
