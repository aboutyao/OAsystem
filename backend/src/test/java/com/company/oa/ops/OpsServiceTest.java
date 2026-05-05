package com.company.oa.ops;

import com.company.oa.BaseMySqlTest;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.error.BusinessException;
import com.company.oa.ops.mapper.AppExceptionLogMapper;
import com.company.oa.ops.mapper.BackupRecordMapper;
import com.company.oa.ops.mapper.JobTaskLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpsServiceTest extends BaseMySqlTest {

    private OpsService service;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM job_task_log WHERE job_code LIKE 'TEST_%'");
        jdbc.update("DELETE FROM app_exception_log WHERE request_id LIKE 'rid-%'");

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        service = new OpsService(
                getMapper(JobTaskLogMapper.class),
                getMapper(AppExceptionLogMapper.class),
                getMapper(BackupRecordMapper.class),
                objectMapper
        );
    }

    @Test
    void seededJobLogsReturnedAndOrdered() {
        PageResponse<Map<String, Object>> result = service.listJobLogs(1, 20, null, null);
        assertThat(result.total()).isGreaterThanOrEqualTo(2L);
        assertThat(result.items()).isNotEmpty();
        assertThat(result.items().get(0)).containsKey("jobCode");
    }

    @Test
    void recordAndQueryJobLog() {
        Timestamp now = Timestamp.from(Instant.now());
        long id = service.recordJobLog("TEST_JOB", "测试任务", OpsService.STATUS_SUCCESS,
                now, now, 1234L, 5L, 0L, null, "SYSTEM");
        assertThat(id).isPositive();

        PageResponse<Map<String, Object>> filtered = service.listJobLogs(1, 20, "TEST_JOB", null);
        assertThat(filtered.total()).isEqualTo(1L);
        assertThat(filtered.items().get(0).get("status")).isEqualTo("SUCCESS");
    }

    @Test
    void recordAndQueryExceptionWithSeverityFilter() {
        service.recordException("rid-1", "/api/test", "GET", 1L,
                "java.lang.RuntimeException", "boom", "stack", "ERROR");
        service.recordException("rid-2", "/api/test", "GET", 1L,
                "java.lang.IllegalArgumentException", "bad", "stack", "WARN");

        PageResponse<Map<String, Object>> errors = service.listExceptions(1, 20, "ERROR");
        assertThat(errors.total()).isEqualTo(1L);
        PageResponse<Map<String, Object>> warns = service.listExceptions(1, 20, "WARN");
        assertThat(warns.total()).isEqualTo(1L);
        PageResponse<Map<String, Object>> all = service.listExceptions(1, 20, null);
        assertThat(all.total()).isEqualTo(2L);
    }

    @Test
    void seededBackupReturnsAndRecordingNew() {
        PageResponse<Map<String, Object>> initial = service.listBackupRecords(1, 20, null, null);
        long initialCount = initial.total();
        assertThat(initialCount).isGreaterThanOrEqualTo(1L);

        Timestamp now = Timestamp.from(Instant.now());
        service.recordBackup("ATTACHMENTS", "/var/backups/files.tar.gz", 1024000L,
                OpsService.STATUS_SUCCESS, now, now, 5000L, null, "MANUAL");

        PageResponse<Map<String, Object>> after = service.listBackupRecords(1, 20, null, null);
        assertThat(after.total()).isEqualTo(initialCount + 1);
    }

    @Test
    void invalidJobLogRequest_throws() {
        assertThatThrownBy(() -> service.recordJobLog(null, null, null, null, null, null, 0, 0, null, null))
                .isInstanceOf(BusinessException.class);
    }
}
