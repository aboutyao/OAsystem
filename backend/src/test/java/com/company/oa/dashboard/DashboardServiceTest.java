package com.company.oa.dashboard;

import com.company.oa.BaseMySqlTest;
import com.company.oa.audit.AuditService;
import com.company.oa.audit.mapper.AuditLoginLogMapper;
import com.company.oa.audit.mapper.AuditOperationLogMapper;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.message.MessageService;
import com.company.oa.message.mapper.MsgMessageMapper;
import com.company.oa.notice.mapper.OaNoticeMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.company.oa.workflow.mapper.WfCcRecordMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import com.company.oa.workflow.mapper.WfTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest extends BaseMySqlTest {

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        // Clean up data from other tests
        jdbc.update("DELETE FROM wf_task");
        jdbc.update("DELETE FROM wf_process_instance");
        jdbc.update("DELETE FROM wf_cc_record");
        jdbc.update("DELETE FROM msg_message");
        jdbc.update("DELETE FROM app_exception_log");

        AuthService authService = mock(AuthService.class);
        when(authService.currentUser()).thenReturn(
                new AuthUser(1L, "admin", "系统管理员", 2L, "总经办",
                        List.of("SUPER_ADMIN"), List.of("*"))
        );
        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        AuditService auditService = new AuditService(
                getMapper(AuditLoginLogMapper.class),
                getMapper(AuditOperationLogMapper.class),
                getMapper(SysConfigMapper.class),
                sequenceService
        );
        MessageService messageService = new MessageService(
                getMapper(MsgMessageMapper.class),
                getMapper(SysConfigMapper.class),
                authService,
                auditService,
                sequenceService
        );
        dashboardService = new DashboardService(
                getMapper(WfTaskMapper.class),
                getMapper(WfProcessInstanceMapper.class),
                getMapper(WfCcRecordMapper.class),
                getMapper(OaNoticeMapper.class),
                authService,
                messageService
        );
    }

    @Test
    void summaryReturnsZeroWhenNoData() {
        Map<String, Object> s = dashboardService.summary();
        assertThat(s).containsKeys("todoCount", "messageCount", "startedCount", "ccCount", "exceptionCount");
        assertThat(((Number) s.get("todoCount")).longValue()).isZero();
        assertThat(((Number) s.get("messageCount")).longValue()).isZero();
        assertThat(((Number) s.get("startedCount")).longValue()).isZero();
        assertThat(((Number) s.get("ccCount")).longValue()).isZero();
    }

    @Test
    void summaryAggregatesAcrossTablesForCurrentUser() {
        Timestamp now = Timestamp.from(Instant.now());

        long instId = 9001L;
        jdbc.update("""
                insert into wf_process_instance (
                    id, process_instance_id, template_id, process_version_id, business_type, business_id, title,
                    starter_id, starter_name_snapshot, starter_dept_id, starter_dept_name_snapshot,
                    current_node_name, status, started_at, ended_at
                ) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,null)
                """,
                instId, "p" + instId, 1L, 1L, "LEAVE", 1L, "测试请假",
                1L, "管理员", 2L, "总经办", "直属上级", "APPROVING", now);

        long taskId = 9101L;
        jdbc.update("""
                insert into wf_task (
                    id, flowable_task_id, process_instance_id, wf_instance_id, node_id, node_name,
                    assignee_id, assignee_name_snapshot, assignee_dept_id, task_type, status,
                    due_at, completed_at, created_at
                ) values (?,?,?,?,?,?,?,?,?,?,?,null,null,?)
                """,
                taskId, "ft1", "p" + instId, instId, "n1", "审批",
                1L, "管理员", 2L, "APPROVE", "PENDING", now);

        long ccId = 9201L;
        jdbc.update("""
                insert into wf_cc_record (id, wf_instance_id, receiver_id, cc_reason, created_by, created_at, read_at)
                values (?,?,?,?,?,?,null)
                """, ccId, instId, 1L, "供参考", 1L, now);

        jdbc.update("""
                insert into msg_message (id, receiver_id, message_type, title, content, business_type,
                    business_id, wf_instance_id, read_status, archive_status, created_at, read_at)
                values (?,?,?,?,?,?,?,?,?,?,?,?)
                """, 9301L, 1L, "TODO", "审批待办", "请处理", "LEAVE", 1L, instId,
                "UNREAD", "NORMAL", now, null);

        Map<String, Object> s = dashboardService.summary();
        assertThat(((Number) s.get("todoCount")).longValue()).isEqualTo(1L);
        assertThat(((Number) s.get("startedCount")).longValue()).isEqualTo(1L);
        assertThat(((Number) s.get("ccCount")).longValue()).isEqualTo(1L);
        assertThat(((Number) s.get("messageCount")).longValue()).isEqualTo(1L);

        assertThat(dashboardService.todos(10)).hasSize(1);
        assertThat(dashboardService.myStarted(10)).hasSize(1);
        assertThat(dashboardService.ccToMe(10)).hasSize(1);
    }

    @Test
    void quickActionsRespectsPermissions() {
        List<Map<String, Object>> all = dashboardService.quickActions();
        assertThat(all).isNotEmpty();
    }
}
