package com.company.oa.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.org.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("""
            select u.id, u.username, u.employee_no as employeeNo, u.real_name as realName,
                   d.dept_name as mainDeptName, u.employee_status as employeeStatus, u.account_status as accountStatus
            from org_user u
            left join org_dept d on d.id = u.main_dept_id
            where u.deleted = 0 and (u.main_dept_id = #{deptId} or exists (
              select 1 from org_user_dept ud where ud.user_id = u.id and ud.dept_id = #{deptId}))
            order by u.id limit #{limit} offset #{offset}
            """)
    List<Map<String, Object>> selectDeptUsers(@Param("deptId") long deptId,
                                               @Param("limit") long limit,
                                               @Param("offset") long offset);

    @Select("""
            <script>
            select u.id, u.username, u.employee_no as employeeNo, u.real_name as realName,
                   d.dept_name as mainDeptName, u.employee_status as employeeStatus, u.account_status as accountStatus
            from org_user u
            left join org_dept d on d.id = u.main_dept_id
            where u.deleted = 0
            <if test="keyword != null">
              and (u.username like #{keyword} or u.real_name like #{keyword} or u.employee_no like #{keyword})
            </if>
            <if test="mainDeptId != null">
              and u.main_dept_id = #{mainDeptId}
            </if>
            <if test="employeeStatus != null and employeeStatus != ''">
              and u.employee_status = #{employeeStatus}
            </if>
            <if test="accountStatus != null and accountStatus != ''">
              and u.account_status = #{accountStatus}
            </if>
            order by u.id limit #{limit} offset #{offset}
            </script>
            """)
    List<Map<String, Object>> selectUserList(@Param("keyword") String keyword,
                                              @Param("limit") long limit,
                                              @Param("offset") long offset,
                                              @Param("mainDeptId") Long mainDeptId,
                                              @Param("employeeStatus") String employeeStatus,
                                              @Param("accountStatus") String accountStatus);

    @Select("""
            select u.id, u.username, u.employee_no as employeeNo, u.real_name as realName,
                   u.mobile, u.email, u.main_dept_id as mainDeptId, d.dept_name as mainDeptName,
                   u.position_id as positionId, p.position_name as positionName,
                   u.rank_id as rankId, r.rank_name as rankName,
                   u.manager_user_id as managerUserId, m.real_name as managerName,
                   u.employee_status as employeeStatus, u.account_status as accountStatus,
                   u.entry_date as entryDate, u.resign_date as resignDate
            from org_user u
            left join org_dept d on d.id = u.main_dept_id
            left join org_position p on p.id = u.position_id
            left join org_rank r on r.id = u.rank_id
            left join org_user m on m.id = u.manager_user_id and m.deleted = 0
            where u.id = #{id} and u.deleted = 0
            """)
    Map<String, Object> selectUserDetailById(@Param("id") long id);

    @Select("""
            select u.id, u.username, u.employee_no as employeeNo, u.real_name as realName, u.mobile, u.email,
                   d.dept_name as mainDeptName, p.position_name as positionName
            from org_user u
            left join org_dept d on d.id = u.main_dept_id
            left join org_position p on p.id = u.position_id
            where u.deleted = 0 and u.account_status = 'ENABLED' and u.employee_status <> 'RESIGNED'
            and (u.real_name like #{keyword} or u.username like #{keyword} or u.mobile like #{keyword})
            order by u.real_name
            """)
    List<Map<String, Object>> selectContactsWithKeyword(@Param("keyword") String keyword);

    @Select("""
            select u.id, u.username, u.employee_no as employeeNo, u.real_name as realName, u.mobile, u.email,
                   d.dept_name as mainDeptName, p.position_name as positionName
            from org_user u
            left join org_dept d on d.id = u.main_dept_id
            left join org_position p on p.id = u.position_id
            where u.deleted = 0 and u.account_status = 'ENABLED' and u.employee_status <> 'RESIGNED'
            order by u.real_name
            """)
    List<Map<String, Object>> selectAllContacts();

    @Select("""
            select u.id, u.username, u.employee_no, u.real_name, d.dept_name, u.employee_status, u.account_status, u.mobile, u.email
            from org_user u left join org_dept d on d.id = u.main_dept_id
            where u.deleted = 0 order by u.id
            """)
    List<Map<String, Object>> selectAllUsersForExport();

    @Select("select ur.role_id from perm_user_role ur where ur.user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") long userId);

    @Select("""
            select u.real_name as realName, u.main_dept_id as deptId, d.dept_name as deptName
            from org_user u
            left join org_dept d on d.id = u.main_dept_id
            where u.id = #{userId} and u.deleted = 0
            """)
    Map<String, Object> selectUserSnapshot(@Param("userId") long userId);

    @Select("select manager_user_id from org_user where id = #{userId} and deleted = 0")
    Long selectManagerUserId(@Param("userId") long userId);

    @Select("""
            select u.main_dept_id as deptId, d.dept_name as deptName
            from org_user u left join org_dept d on d.id = u.main_dept_id
            where u.id = #{userId} and u.deleted = 0
            """)
    Map<String, Object> selectUserDeptSnapshot(@Param("userId") long userId);

    @Select("""
            select u.id as userId, u.username, u.real_name as realName,
                   u.last_login_at as lastLoginAt, d.dept_name as deptName
            from org_user u
            left join org_dept d on d.id = u.main_dept_id
            where u.deleted = 0
              and u.account_status = 'ENABLED'
              and u.last_login_at is not null
              and u.last_login_at >= #{threshold}
            order by u.last_login_at desc
            """)
    List<Map<String, Object>> selectRecentLogins(@Param("threshold") java.time.LocalDateTime threshold);

    @Update("update org_user set login_fail_count = 0, locked_until = null, last_login_at = CURRENT_TIMESTAMP where id = #{userId}")
    int updateLoginSuccess(@Param("userId") long userId);

    @Update("update org_user set login_fail_count = login_fail_count + 1 where id = #{userId}")
    int updateLoginFailCount(@Param("userId") long userId);

    @Update("update org_user set locked_until = #{lockedUntil} where id = #{userId}")
    int updateLockedUntil(@Param("userId") long userId, @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    @Update("update org_user set password_hash = #{passwordHash}, password_changed_at = #{now}, password_expires_at = #{expiresAt}, updated_at = #{now} where id = #{userId}")
    int updatePasswordHash(@Param("userId") long userId, @Param("passwordHash") String passwordHash, @Param("now") java.time.LocalDateTime now, @Param("expiresAt") java.time.LocalDateTime expiresAt);

    @Update("update org_user set totp_secret = #{secret}, totp_enabled = 1, updated_at = now() where id = #{userId}")
    int enableTotp(@Param("userId") long userId, @Param("secret") String secret);

    @Update("update org_user set totp_secret = null, totp_enabled = 0, updated_at = now() where id = #{userId}")
    int disableTotp(@Param("userId") long userId);

    @Select("""
            select u.id from org_user u
            inner join perm_user_role ur on ur.user_id = u.id
            inner join perm_role r on r.id = ur.role_id and r.role_code = #{roleCode} and r.status = 'ENABLED'
            where u.deleted = 0 and u.account_status = 'ENABLED'
            order by ur.created_at asc
            limit 1
            """)
    Long selectUserIdByRoleCode(@Param("roleCode") String roleCode);

    @Select("""
            select u.id from org_user u
            inner join perm_user_role ur on ur.user_id = u.id
            inner join perm_role r on r.id = ur.role_id and r.role_code = #{roleCode} and r.status = 'ENABLED'
            where u.deleted = 0 and u.account_status = 'ENABLED'
            order by ur.created_at asc
            """)
    List<Long> selectAllUserIdsByRoleCode(@Param("roleCode") String roleCode);
}
