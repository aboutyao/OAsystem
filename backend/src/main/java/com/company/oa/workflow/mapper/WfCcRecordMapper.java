package com.company.oa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.wf.WfCcRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface WfCcRecordMapper extends BaseMapper<WfCcRecord> {

    @Select("""
            select cc.id, cc.wf_instance_id as wfInstanceId, inst.title, inst.business_type as businessType,
                   inst.business_id as businessId, inst.starter_name_snapshot as starterName,
                   inst.status as instanceStatus, cc.cc_reason as ccReason,
                   cc.created_at as createdAt, cc.read_at as readAt
            from wf_cc_record cc
            join wf_process_instance inst on inst.id = cc.wf_instance_id
            where cc.receiver_id = #{receiverId}
            order by cc.created_at desc, cc.id desc
            limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectCcToMe(@Param("receiverId") long receiverId,
                                            @Param("limit") long limit,
                                            @Param("offset") long offset);

    @Update("update wf_cc_record set read_at = coalesce(read_at, #{now}) where id = #{id}")
    int markRead(@Param("id") long id, @Param("now") LocalDateTime now);

    @Select("select count(*) from wf_cc_record where receiver_id = #{receiverId} and read_at is null")
    Long countUnreadByReceiver(@Param("receiverId") long receiverId);

    @Select("""
            select cc.id as ccId, cc.wf_instance_id as wfInstanceId, inst.title,
                   inst.business_type as businessType, inst.business_id as businessId,
                   cc.read_at as readAt, cc.created_at as createdAt
            from wf_cc_record cc
            join wf_process_instance inst on inst.id = cc.wf_instance_id
            where cc.receiver_id = #{receiverId}
            order by cc.created_at desc, cc.id desc
            limit #{limit}
            """)
    List<Map<String, Object>> selectDashboardCcToMe(@Param("receiverId") long receiverId, @Param("limit") int limit);
}
