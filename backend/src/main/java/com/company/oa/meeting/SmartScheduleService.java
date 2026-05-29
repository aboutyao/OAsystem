package com.company.oa.meeting;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 智能日程协调服务
 * 会议邀请自动检测冲突，推荐最佳时间
 */
@Service
public class SmartScheduleService {
    private final JdbcTemplate jdbcTemplate;

    public SmartScheduleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 检测会议时间冲突
     */
    public Map<String, Object> checkMeetingConflict(List<Long> userIds, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId) {
        Map<String, Object> result = new HashMap<>();
        result.put("hasConflict", false);
        result.put("conflicts", new ArrayList<>());

        List<Map<String, Object>> conflicts = new ArrayList<>();

        for (Long userId : userIds) {
            // 查询该用户在该时间段的会议
            StringBuilder sql = new StringBuilder(
                "SELECT b.*, u.real_name as user_name FROM meeting_booking b " +
                "JOIN meeting_room_user r ON b.room_id = r.room_id " +
                "JOIN org_user u ON r.user_id = u.id " +
                "WHERE r.user_id = ? AND b.status = 'CONFIRMED' " +
                "AND b.start_time < ? AND b.end_time > ?"
            );
            List<Object> params = new ArrayList<>();
            params.add(userId);
            params.add(endTime);
            params.add(startTime);

            if (excludeBookingId != null) {
                sql.append(" AND b.id != ?");
                params.add(excludeBookingId);
            }

            List<Map<String, Object>> conflictBookings = jdbcTemplate.queryForList(sql.toString(), params.toArray());

            for (Map<String, Object> booking : conflictBookings) {
                Map<String, Object> conflict = new HashMap<>();
                conflict.put("userId", userId);
                conflict.put("userName", booking.get("user_name"));
                conflict.put("bookingId", booking.get("id"));
                conflict.put("meetingTitle", booking.get("title"));
                conflict.put("meetingStart", booking.get("start_time"));
                conflict.put("meetingEnd", booking.get("end_time"));
                conflicts.add(conflict);
            }
        }

        if (!conflicts.isEmpty()) {
            result.put("hasConflict", true);
            result.put("conflicts", conflicts);
        }

        return result;
    }

    /**
     * 推荐最佳会议时间
     */
    public List<Map<String, Object>> recommendMeetingTime(List<Long> userIds, int durationMinutes, LocalDate preferredDate) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // 获取所有用户的忙闲状态
        Map<Long, List<Map<String, Object>>> userBusySlots = new HashMap<>();
        for (Long userId : userIds) {
            userBusySlots.put(userId, getUserBusySlots(userId, preferredDate));
        }

        // 扫描可用时间段
        LocalTime startTime = LocalTime.of(9, 0); // 早上9点
        LocalTime endTime = LocalTime.of(18, 0);   // 下午6点

        while (startTime.plusMinutes(durationMinutes).isBefore(endTime) || startTime.plusMinutes(durationMinutes).equals(endTime)) {
            LocalDateTime slotStart = preferredDate.atTime(startTime);
            LocalDateTime slotEnd = slotStart.plusMinutes(durationMinutes);

            boolean isAvailable = true;
            List<Long> availableUsers = new ArrayList<>();
            List<Long> unavailableUsers = new ArrayList<>();

            for (Long userId : userIds) {
                boolean userFree = isUserFree(userId, slotStart, slotEnd, userBusySlots.get(userId));
                if (userFree) {
                    availableUsers.add(userId);
                } else {
                    unavailableUsers.add(userId);
                    isAvailable = false;
                }
            }

            if (isAvailable) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("startTime", slotStart);
                recommendation.put("endTime", slotEnd);
                recommendation.put("availableCount", availableUsers.size());
                recommendation.put("totalUsers", userIds.size());
                recommendation.put("score", calculateTimeSlotScore(slotStart, availableUsers.size(), userIds.size()));
                recommendations.add(recommendation);
            }

            startTime = startTime.plusMinutes(30); // 每30分钟扫描一次
        }

        // 按评分排序
        recommendations.sort((a, b) -> Double.compare(
            (double) b.get("score"),
            (double) a.get("score")
        ));

        // 返回前5个推荐
        return recommendations.subList(0, Math.min(5, recommendations.size()));
    }

    private List<Map<String, Object>> getUserBusySlots(Long userId, LocalDate date) {
        return jdbcTemplate.queryForList(
            "SELECT start_time, end_time FROM meeting_booking " +
            "WHERE status = 'CONFIRMED' AND DATE(start_time) = ? " +
            "AND id IN (SELECT booking_id FROM meeting_room_user WHERE user_id = ?)",
            date, userId
        );
    }

    private boolean isUserFree(Long userId, LocalDateTime start, LocalDateTime end, List<Map<String, Object>> busySlots) {
        if (busySlots == null) return true;

        for (Map<String, Object> slot : busySlots) {
            LocalDateTime busyStart = ((java.sql.Timestamp) slot.get("start_time")).toLocalDateTime();
            LocalDateTime busyEnd = ((java.sql.Timestamp) slot.get("end_time")).toLocalDateTime();

            if (start.isBefore(busyEnd) && end.isAfter(busyStart)) {
                return false;
            }
        }
        return true;
    }

    private double calculateTimeSlotScore(LocalDateTime startTime, int availableCount, int totalCount) {
        double availabilityScore = (double) availableCount / totalCount * 100;

        // 工作时间加分（10-12点，14-16点最佳）
        int hour = startTime.getHour();
        double timeScore = 0;
        if (hour >= 10 && hour <= 12) timeScore = 20;
        else if (hour >= 14 && hour <= 16) timeScore = 15;
        else if (hour >= 9 && hour <= 18) timeScore = 10;
        else timeScore = 0;

        return availabilityScore + timeScore;
    }

    /**
     * 获取用户日程概览
     */
    public Map<String, Object> getUserScheduleOverview(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> overview = new HashMap<>();

        // 会议
        List<Map<String, Object>> meetings = jdbcTemplate.queryForList(
            "SELECT b.*, r.name as room_name FROM meeting_booking b " +
            "JOIN meeting_room r ON b.room_id = r.id " +
            "WHERE b.status = 'CONFIRMED' AND DATE(b.start_time) BETWEEN ? AND ? " +
            "AND b.id IN (SELECT booking_id FROM meeting_room_user WHERE user_id = ?)",
            startDate, endDate, userId
        );
        overview.put("meetings", meetings);

        // 请假
        List<Map<String, Object>> leaves = jdbcTemplate.queryForList(
            "SELECT * FROM oa_leave WHERE created_by = ? AND status IN ('APPROVED', 'COMPLETED') " +
            "AND DATE(start_at) <= ? AND DATE(end_at) >= ?",
            userId, endDate, startDate
        );
        overview.put("leaves", leaves);

        // 待办
        Long todoCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wf_task WHERE assignee_id = ? AND status = 'PENDING'",
            Long.class, userId
        );
        overview.put("todoCount", todoCount);

        return overview;
    }
}
