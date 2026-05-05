package com.company.oa.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.form.FormFieldRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FormFieldRuleMapper extends BaseMapper<FormFieldRule> {

    @Select("""
            select id, field_code as fieldCode, rule_type as ruleType, rule_expression as ruleExpression,
                   description, status, created_at as createdAt, updated_at as updatedAt
            from form_field_rule where template_id = #{templateId} order by field_code, id
            """)
    List<Map<String, Object>> selectFieldRulesByTemplateId(@Param("templateId") long templateId);

    @Select("""
            <script>
            select r.id, r.template_id as templateId, t.template_code as templateCode,
                   r.field_code as fieldCode, r.rule_type as ruleType,
                   r.rule_expression as ruleExpression, r.description, r.status,
                   r.created_at as createdAt, r.updated_at as updatedAt
            from form_field_rule r
            left join form_template t on t.id = r.template_id
            where 1=1
            <if test='templateId != null'>and r.template_id = #{templateId}</if>
            order by r.template_id, r.field_code, r.id
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectFieldRules(@Param("templateId") Long templateId,
                                                @Param("limit") long limit,
                                                @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from form_field_rule r where 1=1
            <if test='templateId != null'>and r.template_id = #{templateId}</if>
            </script>
            """)
    Long countFieldRules(@Param("templateId") Long templateId);

    @Select("""
            select id from form_field_rule where template_id = #{templateId} and field_code = #{fieldCode} and rule_type = #{ruleType}
            """)
    List<Map<String, Object>> selectByUniqueKey(@Param("templateId") long templateId,
                                                 @Param("fieldCode") String fieldCode,
                                                 @Param("ruleType") String ruleType);

    @Select("""
            select field_code as fieldCode, rule_type as ruleType, rule_expression as ruleExpression
            from form_field_rule where template_id = #{templateId} and status = 'ENABLED'
            """)
    List<Map<String, Object>> selectEnabledRulesByTemplateId(@Param("templateId") long templateId);
}
