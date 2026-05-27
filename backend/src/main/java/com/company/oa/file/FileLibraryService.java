package com.company.oa.file;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.oa.audit.AuditService;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.PaginationHelper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.file.FileInfo;
import com.company.oa.entity.file.FileLibraryFolder;
import com.company.oa.file.mapper.FileInfoMapper;
import com.company.oa.file.mapper.FileLibraryFolderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class FileLibraryService {
    private final FileLibraryFolderMapper folderMapper;
    private final FileInfoMapper fileInfoMapper;
    private final PaginationHelper paginationHelper;
    private final AuthService authService;
    private final AuditService auditService;
    private final SequenceService sequenceService;

    public FileLibraryService(FileLibraryFolderMapper folderMapper, FileInfoMapper fileInfoMapper,
                              PaginationHelper paginationHelper, AuthService authService,
                              AuditService auditService, SequenceService sequenceService) {
        this.folderMapper = folderMapper;
        this.fileInfoMapper = fileInfoMapper;
        this.paginationHelper = paginationHelper;
        this.authService = authService;
        this.auditService = auditService;
        this.sequenceService = sequenceService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> foldersTree() {
        List<Map<String, Object>> rows = folderMapper.selectFolderTree();
        Map<Long, Map<String, Object>> byId = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> node = new LinkedHashMap<>(row);
            node.put("children", new ArrayList<Map<String, Object>>());
            byId.put(((Number) node.get("id")).longValue(), node);
        }
        for (Map<String, Object> node : byId.values()) {
            Object p = node.get("parentId");
            if (p == null) {
                roots.add(node);
                continue;
            }
            Map<String, Object> parent = byId.get(((Number) p).longValue());
            if (parent == null) {
                roots.add(node);
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
            children.add(node);
        }
        return roots;
    }

    @Transactional
    public Map<String, Object> createFolder(FileLibraryDtos.FolderCreateRequest req) {
        AuthUser user = authService.currentUser();
        if (req.parentId() != null) {
            loadFolder(req.parentId());
        }
        long id = sequenceService.nextId("file_library_folder");
        int sort = nextFolderSort(req.parentId());
        FileLibraryFolder entity = new FileLibraryFolder();
        entity.setId(id);
        entity.setParentId(req.parentId());
        entity.setFolderName(req.folderName());
        entity.setSortOrder(sort);
        entity.setStatus("ENABLED");
        entity.setCreatedBy(user.id());
        entity.setDeleted(0);
        folderMapper.insert(entity);
        return loadFolder(id);
    }

    @Transactional
    public Map<String, Object> updateFolder(long id, FileLibraryDtos.FolderUpdateRequest req) {
        loadFolder(id);
        LocalDateTime now = LocalDateTime.now();
        folderMapper.update(null, new LambdaUpdateWrapper<FileLibraryFolder>()
                .eq(FileLibraryFolder::getId, id)
                .eq(FileLibraryFolder::getDeleted, 0)
                .set(FileLibraryFolder::getFolderName, req.folderName())
                .set(FileLibraryFolder::getUpdatedAt, now)
                .setSql("version = version + 1"));
        return loadFolder(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listFiles(long page, long size, Long folderId, String keyword) {
        long[] ps = paginationHelper.clamp(page, size);
        if (folderId != null) {
            loadFolder(folderId);
        }
        long total = Objects.requireNonNullElse(
                fileInfoMapper.countFiles(folderId, keyword),
                0L
        );
        List<Map<String, Object>> items = fileInfoMapper.selectFiles(folderId, keyword, ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public Map<String, Object> createFile(FileLibraryDtos.FileCreateRequest req) {
        AuthUser user = authService.currentUser();
        if (req.folderId() != null) {
            loadFolder(req.folderId());
        }
        long id = sequenceService.nextId("file_info");
        FileInfo entity = new FileInfo();
        entity.setId(id);
        entity.setFolderId(req.folderId());
        entity.setFileName(req.fileName());
        entity.setFileExt(ext(req.fileName()));
        entity.setMimeType(req.mimeType());
        entity.setFileSize(req.fileSize());
        entity.setStorageType("LOCAL");
        entity.setStoragePath("library/" + id + "/" + req.fileName());
        entity.setUploadUserId(user.id());
        entity.setStatus("NORMAL");
        entity.setDeleted(0);
        fileInfoMapper.insert(entity);
        auditService.safeRecordOperation(user.id(), "FILE_CREATE", "FILE", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long id) {
        List<Map<String, Object>> rows = fileInfoMapper.selectFileDetail(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        return new LinkedHashMap<>(rows.get(0));
    }

    @Transactional
    public Map<String, Object> addVersion(long id, FileLibraryDtos.FileVersionRequest req) {
        detail(id);
        AuthUser user = authService.currentUser();
        LocalDateTime now = LocalDateTime.now();
        fileInfoMapper.update(null, new LambdaUpdateWrapper<FileInfo>()
                .eq(FileInfo::getId, id)
                .eq(FileInfo::getDeleted, 0)
                .set(FileInfo::getFileName, req.fileName())
                .set(FileInfo::getFileExt, ext(req.fileName()))
                .set(FileInfo::getMimeType, req.mimeType())
                .set(FileInfo::getFileSize, req.fileSize())
                .set(FileInfo::getUploadUserId, user.id())
                .set(FileInfo::getUpdatedAt, now)
                .setSql("version = version + 1"));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> move(long id, FileLibraryDtos.FileMoveRequest req) {
        detail(id);
        loadFolder(req.folderId());
        LocalDateTime now = LocalDateTime.now();
        fileInfoMapper.update(null, new LambdaUpdateWrapper<FileInfo>()
                .eq(FileInfo::getId, id)
                .eq(FileInfo::getDeleted, 0)
                .set(FileInfo::getFolderId, req.folderId())
                .set(FileInfo::getUpdatedAt, now)
                .setSql("version = version + 1"));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> delete(long id) {
        detail(id);
        LocalDateTime now = LocalDateTime.now();
        fileInfoMapper.update(null, new LambdaUpdateWrapper<FileInfo>()
                .eq(FileInfo::getId, id)
                .eq(FileInfo::getDeleted, 0)
                .set(FileInfo::getStatus, "PENDING_CLEAN")
                .set(FileInfo::getDeleted, 1)
                .set(FileInfo::getUpdatedAt, now)
                .setSql("version = version + 1"));
        auditService.safeRecordOperation(authService.currentUser().id(),
                "FILE_DELETE", "FILE", id, AuditService.SUCCESS, null);
        return Map.of("fileId", id, "status", "PENDING_CLEAN");
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listDeletedFiles(long page, long size, String keyword) {
        long[] ps = paginationHelper.clamp(page, size);
        long total = Objects.requireNonNullElse(
                fileInfoMapper.countDeletedFiles(keyword),
                0L
        );
        List<Map<String, Object>> items = fileInfoMapper.selectDeletedFiles(keyword, ps[1], (ps[0] - 1) * ps[1]);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public Map<String, Object> restoreFile(long id) {
        List<Map<String, Object>> rows = fileInfoMapper.selectDeletedFileById(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在于回收站");
        }
        fileInfoMapper.restoreFile(id);
        auditService.safeRecordOperation(authService.currentUser().id(),
                "FILE_RESTORE", "FILE", id, AuditService.SUCCESS, null);
        return detail(id);
    }

    @Transactional
    public Map<String, Object> deletePermanently(long id) {
        List<Map<String, Object>> rows = fileInfoMapper.selectDeletedFileById(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在于回收站");
        }
        fileInfoMapper.deleteDownloadLogsByFileId(id);
        fileInfoMapper.physicalDeleteFile(id);
        auditService.safeRecordOperation(authService.currentUser().id(),
                "FILE_PERMANENT_DELETE", "FILE", id, AuditService.SUCCESS, null);
        return Map.of("fileId", id, "permanentlyDeleted", true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> downloadLogs(long fileId) {
        detail(fileId);
        return fileInfoMapper.selectDownloadLogs(fileId);
    }

    private Map<String, Object> loadFolder(long id) {
        List<Map<String, Object>> rows = folderMapper.selectFolderById(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件夹不存在");
        }
        return new LinkedHashMap<>(rows.get(0));
    }

    private int nextFolderSort(Long parentId) {
        Integer max = folderMapper.selectMaxSort(parentId);
        return (max == null ? 0 : max) + 10;
    }

    private String ext(String fileName) {
        if (fileName == null) {
            return null;
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(idx + 1).toLowerCase();
    }

}
