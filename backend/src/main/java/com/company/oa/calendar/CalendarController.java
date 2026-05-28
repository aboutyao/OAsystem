package com.company.oa.calendar;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/team-leaves")
    public List<Map<String, Object>> teamLeaves(@RequestParam int year, @RequestParam int month) {
        return calendarService.teamLeaves(year, month);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/meetings")
    public List<Map<String, Object>> meetings(@RequestParam int year, @RequestParam int month) {
        return calendarService.meetings(year, month);
    }
}
