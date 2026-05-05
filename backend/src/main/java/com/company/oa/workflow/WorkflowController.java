package com.company.oa.workflow;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/templates")
    public PageResponse<Map<String, Object>> templates(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return workflowService.listTemplates(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/templates/{id}")
    public Map<String, Object> templateDetail(@PathVariable long id) {
        return workflowService.templateDetail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/instances")
    public Map<String, Object> startInstance(@Valid @RequestBody WorkflowDtos.StartInstanceRequest request) {
        return workflowService.startInstance(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/tasks/todo")
    public PageResponse<Map<String, Object>> todoTasks(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return workflowService.todoTasks(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/tasks/done")
    public PageResponse<Map<String, Object>> doneTasks(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return workflowService.doneTasks(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/instances/started-by-me")
    public PageResponse<Map<String, Object>> startedByMe(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return workflowService.startedByMe(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/instances/cc-to-me")
    public PageResponse<Map<String, Object>> ccToMe(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return workflowService.ccToMe(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/cc/{ccId}/read")
    public Map<String, Object> markCcRead(@PathVariable long ccId) {
        return workflowService.markCcRead(ccId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/cc")
    public Map<String, Object> addCc(@Valid @RequestBody WorkflowDtos.CcAddRequest request) {
        return workflowService.addCc(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/instances/{id}")
    public Map<String, Object> instanceDetail(@PathVariable("id") long wfInstanceId) {
        return workflowService.instanceDetail(wfInstanceId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/instances/{id}/timeline")
    public List<Map<String, Object>> timeline(@PathVariable("id") long wfInstanceId) {
        return workflowService.timeline(wfInstanceId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/instances/{id}/diagram")
    public Map<String, Object> diagram(@PathVariable("id") long wfInstanceId) {
        return workflowService.diagram(wfInstanceId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/tasks/{taskId}/approve")
    public Map<String, Object> approve(
            @PathVariable long taskId,
            @RequestBody(required = false) WorkflowDtos.ApproveRequest request
    ) {
        return workflowService.approveTask(taskId, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/tasks/{taskId}/reject")
    public Map<String, Object> reject(
            @PathVariable long taskId,
            @RequestBody(required = false) WorkflowDtos.RejectRequest request
    ) {
        return workflowService.rejectTask(taskId, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/tasks/{taskId}/transfer")
    public Map<String, Object> transfer(
            @PathVariable long taskId,
            @Valid @RequestBody WorkflowDtos.TransferRequest request
    ) {
        return workflowService.transferTask(taskId, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/tasks/{taskId}/add-sign")
    public Map<String, Object> addSign(
            @PathVariable long taskId,
            @Valid @RequestBody WorkflowDtos.AddSignRequest request
    ) {
        return workflowService.addSign(taskId, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/tasks/{taskId}/remind")
    public Map<String, Object> remind(@PathVariable long taskId) {
        return workflowService.remindTask(taskId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/instances/{id}/withdraw")
    public Map<String, Object> withdraw(@PathVariable("id") long wfInstanceId) {
        return workflowService.withdrawInstance(wfInstanceId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/instances/{id}/terminate")
    public Map<String, Object> terminate(@PathVariable("id") long wfInstanceId) {
        return workflowService.terminateInstance(wfInstanceId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/delegations/me")
    public PageResponse<Map<String, Object>> myDelegations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return workflowService.listMyDelegations(page, size);
    }

    @GetMapping("/delegations")
    @PreAuthorize("hasAnyAuthority('*', 'workflow:delegation:manage')")
    public PageResponse<Map<String, Object>> allDelegations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status
    ) {
        return workflowService.listAllDelegations(page, size, status);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/delegations")
    public Map<String, Object> createDelegation(
            @Valid @RequestBody WorkflowDtos.DelegateCreateRequest request
    ) {
        return workflowService.createDelegation(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @DeleteMapping("/delegations/{id}")
    public Map<String, Object> cancelDelegation(@PathVariable long id) {
        return workflowService.cancelDelegation(id);
    }

    @GetMapping("/exceptions")
    @PreAuthorize("hasAnyAuthority('*', 'workflow:exception:view')")
    public PageResponse<Map<String, Object>> exceptions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return workflowService.listExceptions(page, size);
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyAuthority('*', 'workflow:version:publish')")
    public Map<String, Object> createTemplate(@Valid @RequestBody WorkflowDtos.CreateTemplateRequest request) {
        return workflowService.createTemplate(request);
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAnyAuthority('*', 'workflow:version:publish')")
    public Map<String, Object> updateTemplate(
            @PathVariable long id,
            @Valid @RequestBody WorkflowDtos.UpdateTemplateRequest request
    ) {
        return workflowService.updateTemplate(id, request);
    }

    @PostMapping("/templates/{id}/versions")
    @PreAuthorize("hasAnyAuthority('*', 'workflow:version:publish')")
    public Map<String, Object> createVersion(
            @PathVariable long id,
            @Valid @RequestBody WorkflowDtos.CreateVersionRequest request
    ) {
        return workflowService.createVersion(id, request);
    }

    @PostMapping("/versions/{id}/publish")
    @PreAuthorize("hasAnyAuthority('*', 'workflow:version:publish')")
    public Map<String, Object> publishVersion(@PathVariable long id) {
        return workflowService.publishVersion(id);
    }
}
