package com.company.oa.workflow;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow/task-dependencies")
public class TaskDependencyController {
    private final TaskDependencyService dependencyService;

    public TaskDependencyController(TaskDependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:manage')")
    @PostMapping
    public void createDependency(
            @RequestParam long taskId,
            @RequestParam long dependsOnTaskId,
            @RequestParam(defaultValue = "FINISH_TO_START") String dependencyType) {
        dependencyService.createDependency(taskId, dependsOnTaskId, dependencyType);
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:view')")
    @GetMapping("/{taskId}/can-start")
    public boolean canStart(@PathVariable long taskId) {
        return dependencyService.canStart(taskId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'workflow:view')")
    @GetMapping("/{taskId}/downstream")
    public List<Map<String, Object>> getDownstream(@PathVariable long taskId) {
        return dependencyService.getDownstreamTasks(taskId);
    }
}
