package com.company.oa.system;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final SystemService systemService;
    private final NumberRuleService numberRuleService;
    private final WorkCalendarService workCalendarService;
    private final ImportExportService importExportService;

    public SystemController(
            SystemService systemService,
            NumberRuleService numberRuleService,
            WorkCalendarService workCalendarService,
            ImportExportService importExportService
    ) {
        this.systemService = systemService;
        this.numberRuleService = numberRuleService;
        this.workCalendarService = workCalendarService;
        this.importExportService = importExportService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'permission:view')")
    @GetMapping("/dict-types")
    public PageResponse<Map<String, Object>> dictTypes(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return systemService.dictTypes(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/dict-types")
    public Map<String, Object> createDictType(@Valid @RequestBody SystemDtos.DictTypeCreateRequest request) {
        return systemService.createDictType(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PutMapping("/dict-types/{id}")
    public Map<String, Object> updateDictType(@PathVariable long id, @Valid @RequestBody SystemDtos.DictTypeUpdateRequest request) {
        return systemService.updateDictType(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @DeleteMapping("/dict-types/{id}")
    public Map<String, Object> deleteDictType(@PathVariable long id) {
        return systemService.deleteDictType(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'permission:view')")
    @GetMapping("/dict-types/{code}/items")
    public List<Map<String, Object>> dictItems(@PathVariable String code) {
        return systemService.dictItemsByTypeCode(code);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/dict-items")
    public Map<String, Object> createDictItem(@Valid @RequestBody SystemDtos.DictItemCreateRequest request) {
        return systemService.createDictItem(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PutMapping("/dict-items/{id}")
    public Map<String, Object> updateDictItem(@PathVariable long id, @Valid @RequestBody SystemDtos.DictItemUpdateRequest request) {
        return systemService.updateDictItem(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @DeleteMapping("/dict-items/{id}")
    public Map<String, Object> deleteDictItem(@PathVariable long id) {
        return systemService.deleteDictItem(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'permission:view')")
    @GetMapping("/configs")
    public PageResponse<Map<String, Object>> configs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return systemService.configs(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view', 'permission:view')")
    @GetMapping("/configs/{key}")
    public Map<String, Object> configByKey(@PathVariable String key) {
        return systemService.configByKey(key);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create', 'permission:role:assign')")
    @PutMapping("/configs/{key}")
    public Map<String, Object> updateConfig(@PathVariable String key, @Valid @RequestBody SystemDtos.ConfigUpdateRequest request) {
        return systemService.updateConfig(key, request);
    }

    // ============ 编号规则 ============

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/number-rules")
    public PageResponse<Map<String, Object>> numberRules(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return numberRuleService.list(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/number-rules")
    public Map<String, Object> createNumberRule(@Valid @RequestBody SystemDtos.NumberRuleCreateRequest request) {
        return numberRuleService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/number-rules/{ruleCode}/preview")
    public Map<String, Object> previewNumber(@PathVariable String ruleCode) {
        return Map.of("ruleCode", ruleCode, "next", numberRuleService.generateNext(ruleCode));
    }

    // ============ 工作日历 ============

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/work-calendar")
    public PageResponse<Map<String, Object>> workCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "100") long size
    ) {
        return workCalendarService.list(from, to, page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @PostMapping("/work-calendar")
    public Map<String, Object> upsertWorkCalendar(@Valid @RequestBody SystemDtos.WorkCalendarUpsertRequest request) {
        return workCalendarService.upsert(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'permission:role:assign')")
    @DeleteMapping("/work-calendar/{date}")
    public Map<String, Object> deleteWorkCalendar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return workCalendarService.remove(date);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/work-calendar/count")
    public Map<String, Object> countWorkdays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return Map.of(
                "from", from.toString(),
                "to", to.toString(),
                "workdays", workCalendarService.countWorkdays(from, to)
        );
    }

    // ============ 导入导出任务 ============

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/import-tasks")
    public PageResponse<Map<String, Object>> importTasks(
            @RequestParam(required = false) String businessType,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return importExportService.listImportTasks(businessType, page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/import-tasks/{id}")
    public Map<String, Object> importTaskDetail(@PathVariable long id) {
        return importExportService.getImportTask(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/export-tasks")
    public PageResponse<Map<String, Object>> exportTasks(
            @RequestParam(required = false) String businessType,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return importExportService.listExportTasks(businessType, page, size);
    }
}
