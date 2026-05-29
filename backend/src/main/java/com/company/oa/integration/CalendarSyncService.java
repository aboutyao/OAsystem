package com.company.oa.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 日历同步服务
 * 与Outlook、Google日历双向同步
 */
@Service
public class CalendarSyncService {
    private static final Logger log = LoggerFactory.getLogger(CalendarSyncService.class);
    private final JdbcTemplate jdbcTemplate;

    public CalendarSyncService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 导出日程到iCal格式
     */
    public String exportToICal(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        StringBuilder ical = new StringBuilder();
        ical.append("BEGIN:VCALENDAR\r\n");
        ical.append("VERSION:2.0\r\n");
        ical.append("PRODID:-//OA System//Calendar//CN\r\n");

        // 获取请假记录
        List<Map<String, Object>> leaves = jdbcTemplate.queryForList(
            "SELECT * FROM oa_leave WHERE created_by = ? AND status IN ('APPROVED', 'COMPLETED') AND start_at >= ? AND end_at <= ?",
            userId, startTime, endTime
        );

        for (Map<String, Object> leave : leaves) {
            ical.append("BEGIN:VEVENT\r\n");
            ical.append("DTSTART:").append(formatDate((LocalDateTime) leave.get("start_at"))).append("\r\n");
            ical.append("DTEND:").append(formatDate((LocalDateTime) leave.get("end_at"))).append("\r\n");
            ical.append("SUMMARY:").append(leave.get("leave_type")).append("\r\n");
            ical.append("DESCRIPTION:").append(leave.get("reason")).append("\r\n");
            ical.append("END:VEVENT\r\n");
        }

        // 获取会议记录
        List<Map<String, Object>> meetings = jdbcTemplate.queryForList(
            "SELECT * FROM meeting_booking WHERE user_id = ? AND start_time >= ? AND end_time <= ?",
            userId, startTime, endTime
        );

        for (Map<String, Object> meeting : meetings) {
            ical.append("BEGIN:VEVENT\r\n");
            ical.append("DTSTART:").append(formatDate((LocalDateTime) meeting.get("start_time"))).append("\r\n");
            ical.append("DTEND:").append(formatDate((LocalDateTime) meeting.get("end_time"))).append("\r\n");
            ical.append("SUMMARY:").append(meeting.get("title")).append("\r\n");
            ical.append("LOCATION:").append(meeting.get("room_name")).append("\r\n");
            ical.append("END:VEVENT\r\n");
        }

        ical.append("END:VCALENDAR\r\n");
        return ical.toString();
    }

    /**
     * 从iCal导入日程
     */
    public List<Map<String, Object>> importFromICal(String icalContent) {
        List<Map<String, Object>> events = new ArrayList<>();

        // 简单解析iCal格式
        String[] lines = icalContent.split("\r\n");
        Map<String, Object> currentEvent = null;

        for (String line : lines) {
            if (line.equals("BEGIN:VEVENT")) {
                currentEvent = new HashMap<>();
            } else if (line.equals("END:VEVENT") && currentEvent != null) {
                events.add(currentEvent);
                currentEvent = null;
            } else if (currentEvent != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    currentEvent.put(parts[0], parts[1]);
                }
            }
        }

        return events;
    }

    /**
     * 获取忙闲状态
     */
    public List<Map<String, Object>> getFreeBusy(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> busySlots = new ArrayList<>();

        // 查询请假
        List<Map<String, Object>> leaves = jdbcTemplate.queryForList(
            "SELECT start_at, end_at, leave_type FROM oa_leave WHERE created_by = ? AND status IN ('APPROVED', 'COMPLETED') AND start_at < ? AND end_at > ?",
            userId, end, start
        );

        for (Map<String, Object> leave : leaves) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("start", leave.get("start_at"));
            slot.put("end", leave.get("end_at"));
            slot.put("type", "leave");
            slot.put("title", leave.get("leave_type"));
            busySlots.add(slot);
        }

        // 查询会议
        List<Map<String, Object>> meetings = jdbcTemplate.queryForList(
            "SELECT start_time, end_time, title FROM meeting_booking WHERE user_id = ? AND start_time < ? AND end_time > ?",
            userId, end, start
        );

        for (Map<String, Object> meeting : meetings) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("start", meeting.get("start_time"));
            slot.put("end", meeting.get("end_time"));
            slot.put("type", "meeting");
            slot.put("title", meeting.get("title"));
            busySlots.add(slot);
        }

        return busySlots;
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
    }
}
