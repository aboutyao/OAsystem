package com.company.oa.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.wf.WfTaskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WfTaskRecordMapper extends BaseMapper<WfTaskRecord> {

    @Select("""
            select action, operator_name_snapshot as operatorName, node_name as nodeName, comment, operated_at as operatedAt
            from wf_task_record
            where wf_instance_id = #{wfInstanceId}
            order by operated_at, id
            """)
    List<Map<String, Object>> selectTimeline(@Param("wfInstanceId") long wfInstanceId);

    @Select("select count(*) from wf_task_record where wf_instance_id = #{wfInstanceId} and action = #{action}")
    Long countByInstanceIdAndAction(@Param("wfInstanceId") long wfInstanceId, @Param("action") String action);
}
