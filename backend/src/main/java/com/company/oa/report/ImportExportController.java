package com.company.oa.report;

import com.company.oa.common.api.PageResponse;
import com.company.oa.system.ImportExportService;
import com.company.oa.system.SystemDtos;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ImportExportController {

    private final ImportExportService importExportService;

    public ImportExportController(ImportExportService importExportService) {
        this.importExportService = importExportService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/imports/preview")
    public Map<String, Object> importPreview(@RequestBody Map<String, Object> request) {
        String businessType = String.valueOf(request.getOrDefault("businessType", "UNKNOWN"));
        String fileName = String.valueOf(request.getOrDefault("fileName", "import.xlsx"));
        int totalRows = request.containsKey("totalRows") ? ((Number) request.get("totalRows")).intValue() : 0;
        return Map.of(
                "validRows", totalRows,
                "invalidRows", 0,
                "businessType", businessType,
                "fileName", fileName
        );
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/imports/commit")
    public Map<String, Object> importCommit(@RequestBody Map<String, Object> request) {
        String businessType = String.valueOf(request.getOrDefault("businessType", "UNKNOWN"));
        String fileName = String.valueOf(request.getOrDefault("fileName", "import.xlsx"));
        int totalRows = request.containsKey("totalRows") ? ((Number) request.get("totalRows")).intValue() : 0;
        SystemDtos.ImportTaskCreateRequest req = new SystemDtos.ImportTaskCreateRequest(
                businessType, fileName, null, totalRows, totalRows, 0, "SUCCESS", null
        );
        return importExportService.recordImport(req);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/exports")
    public Map<String, Object> createExport(@RequestBody Map<String, Object> request) {
        String businessType = String.valueOf(request.getOrDefault("businessType", "UNKNOWN"));
        String fileName = businessType + "_export.xlsx";
        SystemDtos.ExportTaskCreateRequest req = new SystemDtos.ExportTaskCreateRequest(
                businessType, null, fileName, null, null, "PENDING", null
        );
        return importExportService.recordExport(req);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/exports")
    public PageResponse<Map<String, Object>> exports(
            @RequestParam(required = false) String businessType,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return importExportService.listExportTasks(businessType, page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/exports/{id}")
    public Map<String, Object> export(@PathVariable Long id) {
        return importExportService.getExportTask(id);
    }
}
