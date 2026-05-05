package com.company.oa.ops;

import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.error.ErrorCode;
import com.company.oa.entity.ops.AppExceptionLog;
import com.company.oa.entity.ops.BackupRecord;
import com.company.oa.entity.ops.JobTaskLog;
import com.company.oa.ops.mapper.AppExceptionLogMapper;
import com.company.oa.ops.mapper.BackupRecordMapper;
import com.company.oa.ops.mapper.JobTaskLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OpsService {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_RUNNING = "RUNNING";

    private final JobTaskLogMapper jobTaskLogMapper;
    private final AppExceptionLogMapper appExceptionLogMapper;
    private final BackupRecordMapper backupRecordMapper;
    private final ObjectMapper objectMapper;

    public OpsService(JobTaskLogMapper jobTaskLogMapper,
                      AppExceptionLogMapper appExceptionLogMapper,
                      BackupRecordMapper backupRecordMapper,
                      ObjectMapper objectMapper) {
        this.jobTaskLogMapper = jobTaskLogMapper;
        this.appExceptionLogMapper = appExceptionLogMapper;
        this.backupRecordMapper = backupRecordMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listJobLogs(long page, long size, String jobCode, String status) {
        long[] ps = clampPage(page, size);
        String effectiveJobCode = blankToNull(jobCode);
        String effectiveStatus = blankToNull(status);
        long total = jobTaskLogMapper.countByConditions(effectiveJobCode, effectiveStatus);
        long offset = (ps[0] - 1) * ps[1];
        List<Map<String, Object>> items = jobTaskLogMapper.selectPageByConditions(effectiveJobCode, effectiveStatus, ps[1], offset);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public long recordJobLog(String jobCode, String jobName, String status, java.sql.Timestamp startAt, java.sql.Timestamp endAt,
                              Long durationMs, long successCount, long failCount, String failReason, String triggeredBy) {
        if (jobCode == null || jobName == null || status == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务信息不完整");
        }
        JobTaskLog entity = new JobTaskLog();
        entity.setJobCode(jobCode);
        entity.setJobName(jobName);
        entity.setStatus(status);
        entity.setStartAt(startAt != null ? startAt.toLocalDateTime() : null);
        entity.setEndAt(endAt != null ? endAt.toLocalDateTime() : null);
        entity.setDurationMs(durationMs);
        entity.setSuccessCount(successCount);
        entity.setFailCount(failCount);
        entity.setFailReason(truncate(failReason, 2000));
        entity.setTriggeredBy(truncate(triggeredBy, 64));
        jobTaskLogMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listExceptions(long page, long size, String severity) {
        long[] ps = clampPage(page, size);
        String effectiveSeverity = blankToNull(severity);
        long total = appExceptionLogMapper.countByConditions(effectiveSeverity);
        long offset = (ps[0] - 1) * ps[1];
        List<Map<String, Object>> items = appExceptionLogMapper.selectPageByConditions(effectiveSeverity, ps[1], offset);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public long recordException(String requestId, String requestUri, String requestMethod, Long userId,
                                 String exceptionClass, String exceptionMessage, String stackTrace, String severity) {
        if (exceptionClass == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "异常类必填");
        }
        AppExceptionLog entity = new AppExceptionLog();
        entity.setRequestId(truncate(requestId, 64));
        entity.setRequestUri(truncate(requestUri, 500));
        entity.setRequestMethod(truncate(requestMethod, 16));
        entity.setUserId(userId);
        entity.setExceptionClass(truncate(exceptionClass, 255));
        entity.setExceptionMessage(truncate(exceptionMessage, 2000));
        entity.setStackTrace(truncate(stackTrace, 8000));
        entity.setSeverity(severity == null ? "ERROR" : severity);
        entity.setOccurredAt(LocalDateTime.now());
        appExceptionLogMapper.insert(entity);
        return entity.getId();
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> listBackupRecords(long page, long size, String backupType, String status) {
        long[] ps = clampPage(page, size);
        String effectiveBackupType = blankToNull(backupType);
        String effectiveStatus = blankToNull(status);
        long total = backupRecordMapper.countByConditions(effectiveBackupType, effectiveStatus);
        long offset = (ps[0] - 1) * ps[1];
        List<Map<String, Object>> items = backupRecordMapper.selectPageByConditions(effectiveBackupType, effectiveStatus, ps[1], offset);
        return new PageResponse<>(ps[0], ps[1], total, items);
    }

    @Transactional
    public long recordBackup(String backupType, String backupPath, Long backupSize, String status,
                              java.sql.Timestamp startedAt, java.sql.Timestamp finishedAt, Long durationMs,
                              String failReason, String triggeredBy) {
        if (backupType == null || status == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "备份记录不完整");
        }
        BackupRecord entity = new BackupRecord();
        entity.setBackupType(backupType);
        entity.setBackupPath(truncate(backupPath, 500));
        entity.setBackupSize(backupSize);
        entity.setStatus(status);
        entity.setStartedAt(startedAt != null ? startedAt.toLocalDateTime() : null);
        entity.setFinishedAt(finishedAt != null ? finishedAt.toLocalDateTime() : null);
        entity.setDurationMs(durationMs);
        entity.setFailReason(truncate(failReason, 2000));
        entity.setTriggeredBy(truncate(triggeredBy, 64));
        backupRecordMapper.insert(entity);
        return entity.getId();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value;
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private long[] clampPage(long page, long size) {
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 20 : size;
        if (s > 100) s = 100;
        return new long[]{p, s};
    }
}