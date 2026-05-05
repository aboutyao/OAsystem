package com.company.oa.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.rule.RuleDefinition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RuleDefinitionMapper extends BaseMapper<RuleDefinition> {

    @Select("""
            select id, rule_code as ruleCode, rule_name as ruleName, rule_type as ruleType,
                   business_type as businessType, description, status, group_id as groupId,
                   created_at as createdAt, updated_at as updatedAt
            from rule_definition
            order by id
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectRules(@Param("limit") long limit, @Param("offset") long offset);

    @Select("select count(*) from rule_definition")
    Long countRules();

    @Select("""
            select id, group_id as groupId, rule_code as ruleCode, rule_name as ruleName, rule_type as ruleType,
                   business_type as businessType, description, status, created_at as createdAt, updated_at as updatedAt
            from rule_definition where id = #{id}
            """)
    List<Map<String, Object>> selectRuleById(@Param("id") long id);

    @Select("select count(*) from rule_definition where group_id = #{groupId}")
    Long countByGroupId(@Param("groupId") long groupId);

    @Select("select count(*) from rule_definition where rule_code = #{code}")
    Long countByCode(@Param("code") String code);
}
