package com.company.oa.form;

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

import java.util.Map;

@RestController
@RequestMapping("/api/forms")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/templates")
    public PageResponse<Map<String, Object>> templates(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return formService.listTemplates(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/templates/{id}")
    public Map<String, Object> templateDetail(@PathVariable long id) {
        return formService.templateDetail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/templates")
    public Map<String, Object> createTemplate(@Valid @RequestBody FormDtos.TemplateCreateRequest request) {
        return formService.createTemplate(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PutMapping("/templates/{id}")
    public Map<String, Object> updateTemplate(@PathVariable long id, @Valid @RequestBody FormDtos.TemplateUpdateRequest request) {
        return formService.updateTemplate(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @DeleteMapping("/templates/{id}")
    public Map<String, Object> deleteTemplate(@PathVariable long id) {
        return formService.deleteTemplate(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/templates/{id}/versions")
    public Map<String, Object> createVersion(
            @PathVariable long id,
            @Valid @RequestBody FormDtos.VersionCreateRequest request
    ) {
        return formService.createVersion(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/versions/{id}")
    public Map<String, Object> versionDetail(@PathVariable long id) {
        return formService.versionDetail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/versions/{id}/publish")
    public Map<String, Object> publishVersion(@PathVariable long id) {
        return formService.publishVersion(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/runtime/{businessType}")
    public Map<String, Object> runtime(@PathVariable String businessType) {
        return formService.runtime(businessType);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/field-rules")
    public PageResponse<Map<String, Object>> fieldRules(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long templateId
    ) {
        return formService.listFieldRules(page, size, templateId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/templates/{id}/field-rules")
    public Map<String, Object> upsertFieldRule(
            @PathVariable long id,
            @Valid @RequestBody FormDtos.FieldRuleUpsertRequest request
    ) {
        return formService.upsertFieldRule(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/snapshots")
    public Map<String, Object> saveSnapshot(@Valid @RequestBody FormDtos.SnapshotCreateRequest request) {
        return formService.saveSnapshot(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/snapshots/latest")
    public Map<String, Object> latestSnapshot(
            @RequestParam String businessType,
            @RequestParam long businessId
    ) {
        return formService.latestSnapshot(businessType, businessId);
    }
}
