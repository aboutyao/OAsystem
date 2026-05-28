package com.company.oa.oa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.oa.entity.oa.OaLeave;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OaLeaveMapper extends BaseMapper<OaLeave> {

    @Update("update oa_leave set status = #{status}, updated_at = #{updatedAt}, version = version + 1 where id = #{id} and deleted = 0")
    int updateStatusById(@Param("id") long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("update oa_leave set status = #{status}, process_instance_id = null, wf_instance_id = null, updated_at = #{updatedAt}, version = version + 1 where id = #{id} and deleted = 0")
    int updateStatusClearFlowKeysById(@Param("id") long id, @Param("status") String status, @Param("updatedAt") LocalDateTime updatedAt);

    @Select("""
            select l.id, l.leave_type as leaveType, l.start_at as startAt, l.end_at as endAt, l.status,
                   u.real_name as userName
            from oa_leave l
            join org_user u on u.id = l.created_by and u.deleted = 0
            where l.deleted = 0
              and l.status = 'APPROVED'
              and l.start_at <= #{endOfMonth}
              and l.end_at >= #{startOfMonth}
            order by l.start_at
            """)
    List<Map<String, Object>> selectTeamLeavesByMonth(@Param("startOfMonth") LocalDateTime startOfMonth,
                                                       @Param("endOfMonth") LocalDateTime endOfMonth);

    /**
     * Select approved leave records for a user within a date range, grouped by leave type.
     * Used for leave balance forecasting and usage rate calculation.
     * Returns {leaveType, totalDays, recordCount, month} for each leave type per month.
     */
    @Select("""
            select l.leave_type as leaveType,
                   sum(l.duration_days) as totalDays,
                   count(*) as recordCount,
                   DATE_FORMAT(l.start_at, '%Y-%m') as month
            from oa_leave l
            where l.deleted = 0
              and l.status = 'APPROVED'
              and l.created_by = #{userId}
              and l.start_at >= #{since}
            group by l.leave_type, DATE_FORMAT(l.start_at, '%Y-%m')
            order by l.leave_type, month
            """)
    List<Map<String, Object>> selectLeaveUsageHistory(@Param("userId") long userId,
                                                       @Param("since") LocalDateTime since);
}
