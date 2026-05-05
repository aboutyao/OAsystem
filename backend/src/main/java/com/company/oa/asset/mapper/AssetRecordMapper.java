package com.company.oa.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.asset.AssetRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AssetRecordMapper extends BaseMapper<AssetRecord> {

    @Select("""
            select r.id, r.asset_id as assetId, r.record_type as recordType,
                   r.from_user_id as fromUserId, fu.real_name as fromUserName,
                   r.to_user_id as toUserId, tu.real_name as toUserName,
                   r.reason, r.operated_by as operatedBy, ou.real_name as operatedByName,
                   r.operated_at as operatedAt
            from asset_record r
            left join org_user fu on fu.id = r.from_user_id
            left join org_user tu on tu.id = r.to_user_id
            left join org_user ou on ou.id = r.operated_by
            where r.asset_id = #{assetId}
            order by r.operated_at desc, r.id desc
            """)
    List<Map<String, Object>> selectRecordsByAssetId(@Param("assetId") long assetId);
}
