package com.company.oa.workflow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/workflow/smart-reminder")
public class SmartReminderController {
    private final SmartReminderService reminderService;

    public SmartReminderController(SmartReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:view')")
    @GetMapping("/analyze/{taskId}")
    public Map<String, Object> analyze(@PathVariable long taskId) {
        return reminderService.analyzeReminderTiming(taskId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:approve')")
    @PostMapping("/send/{taskId}")
    public Map<String, Object> sendReminder(@PathVariable long taskId) {
        return reminderService.sendSmartReminder(taskId);
    }
}
