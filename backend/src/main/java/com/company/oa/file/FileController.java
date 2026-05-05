package com.company.oa.file;

import com.company.oa.common.api.DemoData;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) String fieldCode
    ) {
        return DemoData.map(
                "fileId", 1L,
                "fileName", file.getOriginalFilename(),
                "fileSize", file.getSize(),
                "businessType", businessType,
                "businessId", businessId,
                "fieldCode", fieldCode
        );
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/{id}")
    public Map<String, Object> file(@PathVariable Long id) {
        return DemoData.map("fileId", id, "fileName", "demo.pdf", "fileSize", 1024L);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/{id}/download-logs")
    public Object downloadLogs(@PathVariable Long id) {
        return DemoData.list(DemoData.map("fileId", id, "userId", 1L, "downloadedAt", "2026-04-28T03:00:00+08:00"));
    }
}
