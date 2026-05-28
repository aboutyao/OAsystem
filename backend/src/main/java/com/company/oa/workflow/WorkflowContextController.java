package com.company.oa.workflow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowContextController {

    private final WorkflowContextService contextService;

    public WorkflowContextController(WorkflowContextService contextService) {
        this.contextService = contextService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/tasks/{id}/context")
    public Map<String, Object> getApprovalContext(@PathVariable long id) {
        return contextService.getApprovalContext(id);
    }
}
