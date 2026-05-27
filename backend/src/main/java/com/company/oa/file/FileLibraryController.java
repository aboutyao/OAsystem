package com.company.oa.file;

import com.company.oa.common.api.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file-library")
public class FileLibraryController {
    private final FileLibraryService fileLibraryService;

    public FileLibraryController(FileLibraryService fileLibraryService) {
        this.fileLibraryService = fileLibraryService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/folders")
    public List<Map<String, Object>> folders() {
        return fileLibraryService.foldersTree();
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/folders")
    public Map<String, Object> createFolder(@Valid @RequestBody FileLibraryDtos.FolderCreateRequest request) {
        return fileLibraryService.createFolder(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/folders/{id}")
    public Map<String, Object> updateFolder(@PathVariable long id, @Valid @RequestBody FileLibraryDtos.FolderUpdateRequest request) {
        return fileLibraryService.updateFolder(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/files")
    public PageResponse<Map<String, Object>> files(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long folderId,
            @RequestParam(required = false) String keyword
    ) {
        return fileLibraryService.listFiles(page, size, folderId, keyword);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/files")
    public Map<String, Object> createFile(@Valid @RequestBody FileLibraryDtos.FileCreateRequest request) {
        return fileLibraryService.createFile(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/files/{id}")
    public Map<String, Object> file(@PathVariable long id) {
        return fileLibraryService.detail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/files/{id}/upload")
    public Map<String, Object> uploadFile(@PathVariable long id, @RequestParam("file") MultipartFile file) {
        return fileLibraryService.uploadFileContent(id, file);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/files/{id}/download")
    public void downloadFile(@PathVariable long id, HttpServletResponse response) {
        fileLibraryService.downloadFileContent(id, response);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/files/{id}/versions")
    public Map<String, Object> addVersion(@PathVariable long id, @Valid @RequestBody FileLibraryDtos.FileVersionRequest request) {
        return fileLibraryService.addVersion(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/files/{id}/move")
    public Map<String, Object> move(@PathVariable long id, @Valid @RequestBody FileLibraryDtos.FileMoveRequest request) {
        return fileLibraryService.move(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @DeleteMapping("/files/{id}")
    public Map<String, Object> delete(@PathVariable long id) {
        return fileLibraryService.delete(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/files/{id}/download-logs")
    public List<Map<String, Object>> downloadLogs(@PathVariable long id) {
        return fileLibraryService.downloadLogs(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/recycle-bin")
    public PageResponse<Map<String, Object>> recycleBin(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword
    ) {
        return fileLibraryService.listDeletedFiles(page, size, keyword);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/recycle-bin/{id}/restore")
    public Map<String, Object> restore(@PathVariable long id) {
        return fileLibraryService.restoreFile(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @DeleteMapping("/recycle-bin/{id}")
    public Map<String, Object> deletePermanently(@PathVariable long id) {
        return fileLibraryService.deletePermanently(id);
    }
}
