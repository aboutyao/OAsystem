package com.company.oa.meeting;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meetings/smart-schedule")
public class SmartScheduleController {
    private final SmartScheduleService scheduleService;

    public SmartScheduleController(SmartScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'meeting:view')")
    @PostMapping("/check-conflicts")
    public Map<String, Object> checkConflicts(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> userIds = ((List<Number>) request.get("userIds")).stream().map(Number::longValue).toList();
        LocalDateTime startTime = LocalDateTime.parse((String) request.get("startTime"));
        LocalDateTime endTime = LocalDateTime.parse((String) request.get("endTime"));
        Long excludeId = request.get("excludeBookingId") != null
            ? ((Number) request.get("excludeBookingId")).longValue()
            : null;

        return scheduleService.checkMeetingConflict(userIds, startTime, endTime, excludeId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'meeting:view')")
    @PostMapping("/recommend")
    public List<Map<String, Object>> recommendTime(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> userIds = ((List<Number>) request.get("userIds")).stream().map(Number::longValue).toList();
        int durationMinutes = (int) request.get("durationMinutes");
        LocalDate preferredDate = LocalDate.parse((String) request.get("preferredDate"));

        return scheduleService.recommendMeetingTime(userIds, durationMinutes, preferredDate);
    }

    @PreAuthorize("hasAnyAuthority('*', 'meeting:view')")
    @GetMapping("/overview/{userId}")
    public Map<String, Object> getOverview(
            @PathVariable long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return scheduleService.getUserScheduleOverview(userId, LocalDate.parse(startDate), LocalDate.parse(endDate));
    }
}
