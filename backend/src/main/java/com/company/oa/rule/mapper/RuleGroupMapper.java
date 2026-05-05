package com.company.oa.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.rule.RuleGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RuleGroupMapper extends BaseMapper<RuleGroup> {

    @Select("""
            select id, group_code as groupCode, group_name as groupName,
                   description, status, created_at as createdAt, updated_at as updatedAt
            from rule_group where id = #{id}
            """)
    List<Map<String, Object>> selectGroupById(@Param("id") long id);

    @Select("""
            select g.id, g.group_code as groupCode, g.group_name as groupName,
                   g.description, g.status, g.created_at as createdAt, g.updated_at as updatedAt,
                   (select count(*) from rule_definition d where d.group_id = g.id) as ruleCount
            from rule_group g
            order by g.id
            """)
    List<Map<String, Object>> selectGroupsWithRuleCount();

    @Select("""
            <script>
            select count(*) from rule_group where group_code = #{code}
            <if test='excludeId != null'>and id != #{excludeId}</if>
            </script>
            """)
    Long countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);
}
