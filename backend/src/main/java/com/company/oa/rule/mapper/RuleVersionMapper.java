package com.company.oa.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.rule.RuleVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RuleVersionMapper extends BaseMapper<RuleVersion> {

    @Select("""
            select v.id, v.rule_id as ruleId, v.version_no as versionNo, v.rule_content as ruleContent, v.natural_language as naturalLanguage,
                   v.status, v.effective_at as effectiveAt, v.expired_at as expiredAt, v.published_at as publishedAt, v.published_by as publishedBy,
                   v.change_reason as changeReason, v.created_at as createdAt, d.rule_code as ruleCode, d.business_type as businessType
            from rule_version v
            join rule_definition d on d.id = v.rule_id
            where v.id = #{id}
            """)
    List<Map<String, Object>> selectVersionWithRule(@Param("id") long id);

    @Select("""
            select id, version_no as versionNo, status, natural_language as naturalLanguage,
                   published_at as publishedAt, published_by as publishedBy, change_reason as changeReason,
                   created_at as createdAt
            from rule_version where rule_id = #{ruleId} order by version_no desc
            """)
    List<Map<String, Object>> selectVersionsByRuleId(@Param("ruleId") long ruleId);

    @Select("select coalesce(max(version_no),0) from rule_version where rule_id = #{ruleId}")
    Integer selectMaxVersionNo(@Param("ruleId") long ruleId);
}
