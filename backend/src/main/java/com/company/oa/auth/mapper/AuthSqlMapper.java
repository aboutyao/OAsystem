package com.company.oa.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AuthSqlMapper {

    @Select("""
            select u.id, u.username, u.real_name, u.main_dept_id, d.dept_name as main_dept_name
            from org_user u
            left join org_dept d on d.id = u.main_dept_id
            where u.id = #{userId} and u.deleted = 0
            """)
    Map<String, Object> selectUserWithDept(@Param("userId") long userId);

    @Select("""
            select * from org_user where username = #{username} and deleted = 0
            """)
    Map<String, Object> selectUserByUsername(@Param("username") String username);

    @Select("""
            select r.role_code
            from perm_user_role ur
            join perm_role r on r.id = ur.role_id
            where ur.user_id = #{userId} and r.status = 'ENABLED'
            order by r.sort_order, r.id
            """)
    List<String> selectUserRoles(@Param("userId") long userId);

    @Select("""
            select distinct b.permission_code
            from perm_user_role ur
            join perm_role_button rb on rb.role_id = ur.role_id
            join perm_button b on b.id = rb.button_id
            where ur.user_id = #{userId} and b.status = 'ENABLED'
            order by b.permission_code
            """)
    List<String> selectUserButtonPermissions(@Param("userId") long userId);

    @Select("""
            select distinct m.menu_code
            from perm_user_role ur
            join perm_role_menu rm on rm.role_id = ur.role_id
            join perm_menu m on m.id = rm.menu_id
            where ur.user_id = #{userId} and m.status = 'ENABLED'
            order by m.menu_code
            """)
    List<String> selectUserMenuCodes(@Param("userId") long userId);

    @Select("""
            select m.id, m.parent_id as parentId, m.menu_code as menuCode, m.menu_name as menuName,
                   m.route_path as routePath, m.sort_order as sortOrder
            from perm_menu m
            where m.status = 'ENABLED' and m.visible = 1
            order by m.sort_order, m.id
            """)
    List<Map<String, Object>> selectAllVisibleMenus();

    @Select("""
            select distinct m.id, m.parent_id as parentId, m.menu_code as menuCode, m.menu_name as menuName,
                   m.route_path as routePath, m.sort_order as sortOrder
            from perm_menu m
            inner join perm_role_menu rm on rm.menu_id = m.id
            inner join perm_user_role ur on ur.role_id = rm.role_id
            where ur.user_id = #{userId} and m.status = 'ENABLED' and m.visible = 1
            order by m.sort_order, m.id
            """)
    List<Map<String, Object>> selectMenusByUserId(@Param("userId") long userId);

    @Select("select login_fail_count from org_user where id = #{userId}")
    Integer selectLoginFailCount(@Param("userId") long userId);

    @Select("select password_hash from org_user where id = #{userId} and deleted = 0")
    String selectPasswordHash(@Param("userId") long userId);

    @Select("select config_value from sys_config where config_key = #{key}")
    String selectConfigValue(@Param("key") String key);

    @Select("select totp_secret, totp_enabled from org_user where id = #{userId} and deleted = 0")
    Map<String, Object> selectTotpInfo(@Param("userId") long userId);
}