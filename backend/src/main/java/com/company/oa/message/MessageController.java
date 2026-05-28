package com.company.oa.message;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MessageController {
    private final MessageService messageService;
    private final AuthService authService;

    public MessageController(MessageService messageService, AuthService authService) {
        this.messageService = messageService;
        this.authService = authService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/messages")
    public PageResponse<Map<String, Object>> messages(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String readStatus,
            @RequestParam(required = false) String archiveStatus
    ) {
        return messageService.list(page, size, readStatus, archiveStatus);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/messages/unread-count")
    public Map<String, Object> unreadCount() {
        return messageService.unreadCount();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/messages/grouped")
    public Map<String, Object> grouped(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AuthUser user = authService.currentUser();
        return messageService.getGroupedMessages(user.id(), page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/messages/read-all")
    public Map<String, Object> readAll() {
        AuthUser user = authService.currentUser();
        return messageService.markAllAsRead(user.id());
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PatchMapping("/messages/{id}/read")
    public Map<String, Object> read(@PathVariable long id) {
        return messageService.markRead(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PatchMapping("/messages/batch-read")
    public Map<String, Object> batchRead(@Valid @RequestBody MessageDtos.BatchReadRequest body) {
        return messageService.batchRead(body);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PatchMapping("/messages/{id}/archive")
    public Map<String, Object> archive(@PathVariable long id) {
        return messageService.archive(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @DeleteMapping("/messages/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        messageService.delete(id);
        return Map.of("deleted", true, "id", id);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/notification-settings")
    public Map<String, Object> getSettings() {
        return messageService.getNotificationSettings();
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/notification-settings")
    public Map<String, Object> updateSettings(@RequestBody Map<String, Object> body) {
        return messageService.updateNotificationSettings(body);
    }

}
