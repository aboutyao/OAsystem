package com.company.oa.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.perm.PermUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PermUserRoleMapper extends BaseMapper<PermUserRole> {

    @Select("""
            select r.role_code from perm_user_role ur
            join perm_role r on r.id = ur.role_id
            where ur.user_id = #{userId} and r.status = 'ENABLED'
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") long userId);

    @Select("""
            select rm.menu_id from perm_user_role ur
            join perm_role_menu rm on rm.role_id = ur.role_id
            where ur.user_id = #{userId}
            """)
    List<Long> selectMenuIdsByUserId(@Param("userId") long userId);

    @Select("""
            select distinct b.permission_code from perm_user_role ur
            join perm_role_button rb on rb.role_id = ur.role_id
            join perm_button b on b.id = rb.button_id
            where ur.user_id = #{userId} and b.status = 'ENABLED'
            order by b.permission_code
            """)
    List<String> selectButtonPermissionCodesByUserId(@Param("userId") long userId);

    @Select("""
            select ds.id, ds.scope_type as scopeType, ds.business_type as businessType
            from perm_data_scope ds
            join perm_user_role ur on ur.role_id = ds.role_id
            where ur.user_id = #{userId}
            """)
    List<Map<String, Object>> selectDataScopesByUserId(@Param("userId") long userId);

    @Select("""
            select fp.business_type as businessType, fp.field_code as fieldCode,
                   fp.visible, fp.editable, fp.required, fp.masked
            from perm_field_permission fp
            join perm_user_role ur on ur.role_id = fp.role_id
            where ur.user_id = #{userId}
            """)
    List<Map<String, Object>> selectFieldPermissionsByUserId(@Param("userId") long userId);

    @Select("""
            select ur.user_id from perm_user_role ur
            join perm_role r on r.id = ur.role_id
            where r.role_code = #{roleCode}
            limit 1
            """)
    Long findFirstUserIdByRoleCode(@Param("roleCode") String roleCode);
}
