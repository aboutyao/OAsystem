package com.company.oa.audit;

import com.company.oa.BaseMySqlTest;
import com.company.oa.audit.mapper.AuditLoginLogMapper;
import com.company.oa.audit.mapper.AuditOperationLogMapper;
import com.company.oa.common.api.PageResponse;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditServiceTest extends BaseMySqlTest {

    private AuditService service;

    @BeforeEach
    void setUp() {
        // Clean up audit log tables to prevent pollution from other tests
        jdbc.update("DELETE FROM audit_login_log");
        jdbc.update("DELETE FROM audit_operation_log");

        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        service = new AuditService(
                getMapper(AuditLoginLogMapper.class),
                getMapper(AuditOperationLogMapper.class),
                getMapper(SysConfigMapper.class),
                sequenceService
        );
    }

    @Test
    void recordsLoginSuccessAndFailure() {
        service.recordLoginSuccess(1L, "admin", "127.0.0.1", "JUnit");
        service.recordLoginFailure("hacker", "账号或密码错误", "10.0.0.1", "JUnit");
        service.recordLoginFailure("admin", "账号已锁定，请稍后再试", "10.0.0.2", "JUnit");

        PageResponse<Map<String, Object>> all = service.listLoginLogs(1, 20, null, null);
        assertThat(all.total()).isEqualTo(3L);
        assertThat(all.items()).hasSize(3);

        PageResponse<Map<String, Object>> failed = service.listLoginLogs(1, 20, null, "FAILED");
        assertThat(failed.total()).isEqualTo(2L);

        PageResponse<Map<String, Object>> byName = service.listLoginLogs(1, 20, "admin", null);
        assertThat(byName.total()).isEqualTo(2L);
    }

    @Test
    void recordsOperationLog() {
        service.recordOperation(
                "req-1", 1L, "EXPENSE_SUBMIT", "EXPENSE", 100L,
                "POST", "/api/oa/expenses/100/submit", "{\"amount\":1000}",
                "SUCCESS", null, "127.0.0.1"
        );
        service.recordOperation(
                "req-2", 1L, "EXPENSE_SUBMIT", "EXPENSE", 101L,
                "POST", "/api/oa/expenses/101/submit", "{\"amount\":2000}",
                "FAILED", "boom", "127.0.0.1"
        );

        PageResponse<Map<String, Object>> all = service.listOperationLogs(1, 20, null, null, null);
        assertThat(all.total()).isEqualTo(2L);

        PageResponse<Map<String, Object>> succ = service.listOperationLogs(1, 20, "EXPENSE", "SUCCESS", null);
        assertThat(succ.total()).isEqualTo(1L);

        PageResponse<Map<String, Object>> byOp = service.listOperationLogs(1, 20, null, null, 1L);
        assertThat(byOp.total()).isEqualTo(2L);
    }
}
