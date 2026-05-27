package com.company.oa.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.oa.OaSealApply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface OaSealApplyMapper extends BaseMapper<OaSealApply> {

    @Update("update oa_seal_apply set status = #{status}, updated_at = #{updatedAt}, version = version + 1 where id = #{id} and deleted = 0")
    int updateStatusById(@Param("id") long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("update oa_seal_apply set status = #{status}, process_instance_id = null, wf_instance_id = null, updated_at = #{updatedAt}, version = version + 1 where id = #{id} and deleted = 0")
    int updateStatusClearFlowKeysById(@Param("id") long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);
}
