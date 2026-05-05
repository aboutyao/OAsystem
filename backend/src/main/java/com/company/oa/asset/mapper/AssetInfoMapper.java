package com.company.oa.asset.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.asset.AssetInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AssetInfoMapper extends BaseMapper<AssetInfo> {

    @Select("""
            select a.id, a.asset_no as assetNo, a.asset_name as assetName, a.asset_category as assetCategory, a.model,
                   a.purchase_date as purchaseDate, a.purchase_amount as purchaseAmount,
                   a.responsible_user_id as responsibleUserId, ru.real_name as responsibleUserName,
                   a.dept_id as deptId, d.dept_name as deptName,
                   a.status, a.remark, a.created_at as createdAt, a.updated_at as updatedAt, a.version
            from asset_info a
            left join org_user ru on ru.id = a.responsible_user_id
            left join org_dept d on d.id = a.dept_id
            where a.id = #{id} and a.deleted = 0
            """)
    Map<String, Object> selectAssetDetail(@Param("id") long id);

    @Select("""
            select a.id, a.asset_no as assetNo, a.asset_name as assetName, a.asset_category as assetCategory, a.model,
                   a.purchase_date as purchaseDate, a.purchase_amount as purchaseAmount,
                   a.responsible_user_id as responsibleUserId, ru.real_name as responsibleUserName,
                   a.dept_id as deptId, d.dept_name as deptName,
                   a.status, a.remark, a.created_at as createdAt, a.updated_at as updatedAt
            from asset_info a
            left join org_user ru on ru.id = a.responsible_user_id
            left join org_dept d on d.id = a.dept_id
            ${ew.customSqlSegment}
            """)
    List<Map<String, Object>> selectAssetList(@Param("ew") Wrapper<AssetInfo> wrapper);
}
