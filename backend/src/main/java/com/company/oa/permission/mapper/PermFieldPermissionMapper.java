package com.company.oa.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.oa.entity.perm.PermFieldPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface PermFieldPermissionMapper extends BaseMapper<PermFieldPermission> {

    @Select("""
            select fp.id, fp.role_id as roleId, r.role_name as roleName,
                   fp.business_type as businessType, fp.field_code as fieldCode,
                   fp.visible, fp.editable, fp.required, fp.masked
            from perm_field_permission fp
            left join perm_role r on r.id = fp.role_id
            where fp.id = #{id}
            """)
    Map<String, Object> selectWithRoleNameById(@Param("id") long id);

    @Select("""
            <script>
            select fp.id, fp.role_id as roleId, r.role_name as roleName,
                   fp.business_type as businessType, fp.field_code as fieldCode,
                   fp.visible, fp.editable, fp.required, fp.masked
            from perm_field_permission fp
            left join perm_role r on r.id = fp.role_id
            <where>
                <if test="roleId != null">and fp.role_id = #{roleId}</if>
                <if test="businessType != null and businessType != ''">and fp.business_type = #{businessType}</if>
            </where>
            order by fp.role_id, fp.business_type, fp.field_code
            </script>
            """)
    Page<Map<String, Object>> selectWithRoleNamePage(Page<Map<String, Object>> page,
                                                      @Param("roleId") Long roleId,
                                                      @Param("businessType") String businessType);
}
