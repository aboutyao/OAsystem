package com.company.oa.oa;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oa")
public class EditHistoryController {

    private final EditHistoryService editHistoryService;

    public EditHistoryController(EditHistoryService editHistoryService) {
        this.editHistoryService = editHistoryService;
    }

    /**
     * Return all edit history versions for a given OA entity.
     *
     * GET /api/oa/history?entityType=OA_LEAVE&entityId=123
     */
    @PreAuthorize("hasAnyAuthority('*', 'oa:view')")
    @GetMapping("/history")
    public List<Map<String, Object>> history(
            @RequestParam String entityType,
            @RequestParam Long entityId
    ) {
        return editHistoryService.getHistory(entityType, entityId);
    }
}
