package com.company.oa.notice;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category
    ) {
        return noticeService.list(page, size, mine, status, category);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id) {
        return noticeService.detail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody NoticeDtos.NoticeCreateRequest request) {
        return noticeService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody NoticeDtos.NoticeUpdateRequest request) {
        return noticeService.update(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/publish")
    public Map<String, Object> publish(@PathVariable long id) {
        return noticeService.publish(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/withdraw")
    public Map<String, Object> withdraw(@PathVariable long id) {
        return noticeService.withdraw(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable long id) {
        return noticeService.markRead(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable long id) {
        return noticeService.confirmRead(id);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/read-stats")
    public Map<String, Object> readStats(@PathVariable long id) {
        return noticeService.readStats(id);
    }
}
