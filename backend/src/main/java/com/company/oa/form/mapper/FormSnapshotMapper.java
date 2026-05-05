package com.company.oa.form.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.form.FormSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FormSnapshotMapper extends BaseMapper<FormSnapshot> {

    @Select("""
            select id, template_id as templateId, version_id as versionId, data_json as dataJson,
                   created_at as createdAt
            from form_snapshot where business_type = #{businessType} and business_id = #{businessId}
            order by created_at desc, id desc
            limit 1
            """)
    List<Map<String, Object>> selectLatestSnapshot(@Param("businessType") String businessType,
                                                    @Param("businessId") long businessId);
}
