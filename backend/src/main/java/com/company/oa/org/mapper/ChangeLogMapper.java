package com.company.oa.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.org.ChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChangeLogMapper extends BaseMapper<ChangeLog> {

    @Select("""
            <script>
            select c.id, c.target_type as targetType, c.target_id as targetId,
                   c.change_type as changeType, c.before_data as beforeData, c.after_data as afterData,
                   c.reason, c.operator_id as operatorId, u.real_name as operatorName, c.operated_at as operatedAt
            from org_change_log c
            left join org_user u on u.id = c.operator_id
            where 1=1
            <if test="targetType != null">
              and c.target_type = #{targetType}
            </if>
            <if test="changeType != null">
              and c.change_type = #{changeType}
            </if>
            order by c.operated_at desc, c.id desc
            limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectChangeLogList(@Param("targetType") String targetType,
                                                    @Param("changeType") String changeType,
                                                    @Param("limit") long limit,
                                                    @Param("offset") long offset);

    @Select("""
            <script>
            select count(*) from org_change_log c
            where 1=1
            <if test="targetType != null">
              and c.target_type = #{targetType}
            </if>
            <if test="changeType != null">
              and c.change_type = #{changeType}
            </if>
            </script>
            """)
    long selectChangeLogCount(@Param("targetType") String targetType,
                               @Param("changeType") String changeType);
}
