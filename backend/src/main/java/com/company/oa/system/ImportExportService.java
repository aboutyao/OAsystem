package com.company.oa.system;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.common.service.SequenceService;
import com.company.oa.entity.system.SysExportTask;
import com.company.oa.entity.system.SysImportTask;
import com.company.oa.system.mapper.SysExportTaskMapper;
import com.company.oa.system.mapper.SysImportTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImportExportService {

    private final SysImportTaskMapper importTaskMapper;
    private final SysExportTaskMapper exportTaskMapper;
    private final AuthService auth;
    private final SequenceService sequenceService;

    public ImportExportService(SysImportTaskMapper importTaskMapper, SysExportTaskMapper exportTaskMapper,
                               AuthService auth, SequenceService sequenceService) {
        this.importTaskMapper = importTaskMapper;
        this.exportTaskMapper = exportTaskMapper;
        this.auth = auth;
        this.sequenceService = sequenceService;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listImportTasks(String businessType, long page, long size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 20 : Math.min(size, 100);
        Long total = importTaskMapper.countImportTasks(businessType);
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = importTaskMapper.selectImportTasks(businessType, s, (p - 1) * s);
        return new PageResponse<>(p, s, t, items);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listExportTasks(String businessType, long page, long size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 20 : Math.min(size, 100);
        Long total = exportTaskMapper.countExportTasks(businessType);
        long t = total == null ? 0L : total;
        List<Map<String, Object>> items = exportTaskMapper.selectExportTasks(businessType, s, (p - 1) * s);
        return new PageResponse<>(p, s, t, items);
    }

    @Transactional
    public Map<String, Object> recordImport(SystemDtos.ImportTaskCreateRequest req) {
        AuthUser user = auth.currentUser();
        long id = sequenceService.nextId("sys_import_task");
        LocalDateTime now = LocalDateTime.now();
        String code = "IMP-" + System.currentTimeMillis() + "-" + id;
        SysImportTask entity = new SysImportTask();
        entity.setId(id);
        entity.setTaskCode(code);
        entity.setBusinessType(req.businessType());
        entity.setFileName(req.fileName());
        entity.setFileSize(req.fileSize());
        entity.setTotalRows(req.totalRows() == null ? 0 : req.totalRows());
        entity.setSuccessRows(req.successRows() == null ? 0 : req.successRows());
        entity.setFailedRows(req.failedRows() == null ? 0 : req.failedRows());
        entity.setStatus(req.status() == null ? "SUCCESS" : req.status());
        entity.setErrorSummary(req.errorSummary());
        entity.setSubmittedBy(user.id());
        entity.setSubmittedAt(now);
        entity.setFinishedAt(now);
        importTaskMapper.insert(entity);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("taskCode", code);
        return m;
    }

    @Transactional
    public Map<String, Object> recordExport(SystemDtos.ExportTaskCreateRequest req) {
        AuthUser user = auth.currentUser();
        long id = sequenceService.nextId("sys_export_task");
        LocalDateTime now = LocalDateTime.now();
        String code = "EXP-" + System.currentTimeMillis() + "-" + id;
        SysExportTask entity = new SysExportTask();
        entity.setId(id);
        entity.setTaskCode(code);
        entity.setBusinessType(req.businessType());
        entity.setFilterJson(req.filterJson());
        entity.setFileName(req.fileName());
        entity.setFileSize(req.fileSize());
        entity.setRowCount(req.rowCount() == null ? 0 : req.rowCount());
        entity.setStatus(req.status() == null ? "SUCCESS" : req.status());
        entity.setErrorSummary(req.errorSummary());
        entity.setSubmittedBy(user.id());
        entity.setSubmittedAt(now);
        entity.setFinishedAt(now);
        entity.setDownloadCount(0);
        exportTaskMapper.insert(entity);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("taskCode", code);
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getImportTask(long id) {
        List<Map<String, Object>> rows = importTaskMapper.selectImportTaskById(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "导入任务不存在");
        }
        return new LinkedHashMap<>(rows.get(0));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getExportTask(long id) {
        List<Map<String, Object>> rows = exportTaskMapper.selectExportTaskById(id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "导出任务不存在");
        }
        return new LinkedHashMap<>(rows.get(0));
    }
}
