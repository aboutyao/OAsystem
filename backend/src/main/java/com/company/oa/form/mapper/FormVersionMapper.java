package com.company.oa.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.form.FormVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FormVersionMapper extends BaseMapper<FormVersion> {

    @Select("""
            select id, template_id as templateId, version_no as versionNo,
                   fields_json as fieldsJson, layout_json as layoutJson, status,
                   change_reason as changeReason, published_at as publishedAt,
                   published_by as publishedBy, created_at as createdAt
            from form_version where id = #{id}
            """)
    List<Map<String, Object>> selectVersionById(@Param("id") long id);

    @Select("""
            select id, version_no as versionNo, status, change_reason as changeReason,
                   published_at as publishedAt, published_by as publishedBy,
                   created_at as createdAt
            from form_version where template_id = #{templateId} order by version_no desc
            """)
    List<Map<String, Object>> selectVersionsByTemplateId(@Param("templateId") long templateId);
}
