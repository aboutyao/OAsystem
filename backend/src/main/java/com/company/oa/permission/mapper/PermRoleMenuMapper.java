package com.company.oa.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.perm.PermRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermRoleMenuMapper extends BaseMapper<PermRoleMenu> {

    @Select("select menu_id from perm_role_menu where role_id = #{roleId} order by menu_id")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") long roleId);
}
