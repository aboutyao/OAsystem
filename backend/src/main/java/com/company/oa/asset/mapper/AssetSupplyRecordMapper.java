package com.company.oa.asset.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.asset.AssetSupplyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AssetSupplyRecordMapper extends BaseMapper<AssetSupplyRecord> {

    @Select("""
            select r.id, r.supply_id as supplyId, s.supply_code as supplyCode, s.supply_name as supplyName,
                   r.record_type as recordType, r.quantity, r.user_id as userId, u.real_name as userName,
                   r.reason, r.operated_by as operatedBy, ou.real_name as operatedByName, r.operated_at as operatedAt
            from asset_supply_record r
            left join asset_supply s on s.id = r.supply_id
            left join org_user u on u.id = r.user_id
            left join org_user ou on ou.id = r.operated_by
            ${ew.customSqlSegment}
            order by r.operated_at desc, r.id desc
            limit 500
            """)
    List<Map<String, Object>> selectRecordsWithJoins(@Param("ew") Wrapper<AssetSupplyRecord> wrapper);
}
