package com.company.oa.oa.leave;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.oa.entity.oa.OaLeave;
import com.company.oa.oa.mapper.OaLeaveMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks for overlapping approved leaves for a given user.
 */
@Component
public class LeaveConflictCheck {

    private final OaLeaveMapper oaLeaveMapper;

    public LeaveConflictCheck(OaLeaveMapper oaLeaveMapper) {
        this.oaLeaveMapper = oaLeaveMapper;
    }

    /**
     * Find approved leaves that overlap with the given time range for a specific user.
     *
     * @param userId  the user to check
     * @param startAt the proposed leave start
     * @param endAt   the proposed leave end
     * @param excludeLeaveId optional leave ID to exclude (for editing an existing leave)
     * @return list of conflicting leave records as maps
     */
    public List<Map<String, Object>> checkConflicts(long userId, LocalDateTime startAt, LocalDateTime endAt, Long excludeLeaveId) {
        // Overlap condition: existing.startAt <= new.endAt AND existing.endAt >= new.startAt
        LambdaQueryWrapper<OaLeave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaLeave::getCreatedBy, userId);
        wrapper.eq(OaLeave::getDeleted, 0);
        wrapper.in(OaLeave::getStatus, "APPROVED", "APPROVING");
        wrapper.le(OaLeave::getStartAt, endAt);
        wrapper.ge(OaLeave::getEndAt, startAt);
        if (excludeLeaveId != null) {
            wrapper.ne(OaLeave::getId, excludeLeaveId);
        }

        List<OaLeave> conflicts = oaLeaveMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (OaLeave leave : conflicts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", leave.getId());
            item.put("leaveType", leave.getLeaveType());
            item.put("startAt", leave.getStartAt());
            item.put("endAt", leave.getEndAt());
            item.put("durationDays", leave.getDurationDays());
            item.put("status", leave.getStatus());
            result.add(item);
        }
        return result;
    }
}
