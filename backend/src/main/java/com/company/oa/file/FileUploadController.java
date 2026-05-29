package com.company.oa.file;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file-upload")
public class FileUploadController {
    private final MinioStorageService minioStorageService;
    private final AuthService authService;

    public FileUploadController(MinioStorageService minioStorageService, AuthService authService) {
        this.minioStorageService = minioStorageService;
        this.authService = authService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        AuthUser user = authService.currentUser();
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = "uploads/" + UUID.randomUUID().toString() + extension;

        try {
            minioStorageService.upload(objectName, file.getInputStream(),
                    file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件上传失败: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", objectName);
        result.put("name", originalFilename);
        result.put("size", file.getSize());
        result.put("contentType", file.getContentType());
        return result;
    }
}
