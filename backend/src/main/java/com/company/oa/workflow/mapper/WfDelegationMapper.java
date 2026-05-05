package com.company.oa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.wf.WfDelegation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface WfDelegationMapper extends BaseMapper<WfDelegation> {

    @Select("""
            select d.id, d.delegator_id as delegatorId, du.real_name as delegatorName,
                   d.delegatee_id as delegateeId, eu.real_name as delegateeName,
                   d.business_scope as businessScope, d.start_at as startAt, d.end_at as endAt,
                   d.status, d.reason, d.created_at as createdAt, d.cancelled_at as cancelledAt
            from wf_delegation d
            left join org_user du on du.id = d.delegator_id
            left join org_user eu on eu.id = d.delegatee_id
            where d.delegator_id = #{userId} or d.delegatee_id = #{userId}
            order by d.created_at desc, d.id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectMyDelegations(@Param("userId") long userId,
                                                    @Param("limit") long limit,
                                                    @Param("offset") long offset);

    @Select("select count(*) from wf_delegation where delegator_id = #{userId} or delegatee_id = #{userId}")
    Long countMyDelegations(@Param("userId") long userId);

    @Select("""
            select d.id, d.delegator_id as delegatorId, du.real_name as delegatorName,
                   d.delegatee_id as delegateeId, eu.real_name as delegateeName,
                   d.business_scope as businessScope, d.start_at as startAt, d.end_at as endAt,
                   d.status, d.reason, d.created_at as createdAt
            from wf_delegation d
            left join org_user du on du.id = d.delegator_id
            left join org_user eu on eu.id = d.delegatee_id
            <where>
                <if test="status != null and status != ''">and d.status = #{status}</if>
            </where>
            order by d.created_at desc, d.id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectAllDelegations(@Param("status") String status,
                                                    @Param("limit") long limit,
                                                    @Param("offset") long offset);

    @Select("<script>" +
            "select count(*) from wf_delegation d where 1=1" +
            "<if test='status != null and status != \"\"'> and d.status = #{status}</if>" +
            "</script>")
    Long countAllDelegations(@Param("status") String status);

    @Select("""
            select delegatee_id from wf_delegation
            where delegator_id = #{delegatorId} and status = 'ACTIVE'
              and start_at <= #{now} and end_at >= #{now}
            order by created_at desc
            limit 1
            """)
    Long findActiveDelegateeFor(@Param("delegatorId") long delegatorId, @Param("now") LocalDateTime now);
}
