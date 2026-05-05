package com.company.oa.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.perm.PermRoleButton;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermRoleButtonMapper extends BaseMapper<PermRoleButton> {

    @Select("select button_id from perm_role_button where role_id = #{roleId} order by button_id")
    List<Long> selectButtonIdsByRoleId(@Param("roleId") long roleId);
}
