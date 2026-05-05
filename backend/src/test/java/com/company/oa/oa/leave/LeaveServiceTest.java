package com.company.oa.oa.leave;

import com.company.oa.BaseMySqlTest;
import com.company.oa.audit.AuditService;
import com.company.oa.audit.mapper.AuditLoginLogMapper;
import com.company.oa.audit.mapper.AuditOperationLogMapper;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.oa.mapper.OaLeaveMapper;
import com.company.oa.system.WorkCalendarService;
import com.company.oa.system.mapper.SysConfigMapper;
import com.company.oa.system.mapper.SysWorkCalendarMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.workflow.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeaveServiceTest extends BaseMySqlTest {

    private AuthService authService;
    private WorkflowService workflowService;
    private LeaveService leaveService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        workflowService = mock(WorkflowService.class);
        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        AuditService auditService = new AuditService(getMapper(AuditLoginLogMapper.class),
                getMapper(AuditOperationLogMapper.class), getMapper(SysConfigMapper.class), sequenceService);
        WorkCalendarService workCalendarService = new WorkCalendarService(getMapper(SysWorkCalendarMapper.class), sequenceService);
        leaveService = new LeaveService(getMapper(OaLeaveMapper.class), getMapper(SysConfigMapper.class),
                authService, workflowService, auditService, workCalendarService, sequenceService);
    }

    private static AuthUser admin() {
        return new AuthUser(1L, "admin", "系统管理员", 2L, "总经办", List.of("SUPER_ADMIN"), List.of("*"));
    }

    @Test
    void createSubmitWithdraw_setsWithdrawnAndClearsWorkflowIds() {
        when(authService.currentUser()).thenReturn(admin());
        when(workflowService.startInstance(any(WorkflowDtos.StartInstanceRequest.class))).thenReturn(Map.of(
                "processInstanceId", 100L,
                "wfInstanceId", 99L,
                "currentNodeName", "直属上级审批"
        ));

        Map<String, Object> created = leaveService.create(new LeaveDtos.LeaveCreateRequest(
                "ANNUAL",
                LocalDateTime.parse("2026-04-28T09:00:00"),
                LocalDateTime.parse("2026-04-28T18:00:00"),
                BigDecimal.valueOf(8),
                BigDecimal.ONE,
                "休假",
                null
        ));
        long id = ((Number) created.get("id")).longValue();
        assertThat(created.get("status")).isEqualTo("DRAFT");

        Map<String, Object> submitted = leaveService.submit(id);
        assertThat(submitted.get("status")).isEqualTo("APPROVING");
        assertThat(submitted.get("currentNodeName")).isEqualTo("直属上级审批");
        verify(workflowService).startInstance(any(WorkflowDtos.StartInstanceRequest.class));

        leaveService.withdrawLeave(id);
        verify(workflowService).withdrawInstance(99L);

        Map<String, Object> after = leaveService.detail(id);
        assertThat(after.get("status")).isEqualTo("WITHDRAWN");
        assertThat(after.get("wfInstanceId")).isNull();
        assertThat(after.get("processInstanceId")).isNull();
    }

    @Test
    void cancelFromDraft_setsCancelledWithoutWorkflowCall() {
        when(authService.currentUser()).thenReturn(admin());
        Map<String, Object> created = leaveService.create(new LeaveDtos.LeaveCreateRequest(
                "SICK",
                LocalDateTime.parse("2026-05-01T09:00:00"),
                LocalDateTime.parse("2026-05-01T12:00:00"),
                BigDecimal.valueOf(3),
                new BigDecimal("0.375"),
                "就医",
                null
        ));
        long id = ((Number) created.get("id")).longValue();

        Map<String, Object> cancelled = leaveService.cancelLeave(id);
        assertThat(cancelled.get("status")).isEqualTo("CANCELLED");
        verify(workflowService, never()).terminateInstance(anyLong());
    }

    @Test
    void submitTwiceOnSameLeave_fails() {
        when(authService.currentUser()).thenReturn(admin());
        when(workflowService.startInstance(any(WorkflowDtos.StartInstanceRequest.class))).thenReturn(Map.of(
                "processInstanceId", 100L,
                "wfInstanceId", 1L,
                "currentNodeName", "审批"
        ));
        long id = ((Number) leaveService.create(new LeaveDtos.LeaveCreateRequest(
                "PERSONAL",
                LocalDateTime.parse("2026-06-01T09:00:00"),
                LocalDateTime.parse("2026-06-01T10:00:00"),
                BigDecimal.ONE,
                new BigDecimal("0.125"),
                null,
                null
        )).get("id")).longValue();
        leaveService.submit(id);

        assertThatThrownBy(() -> leaveService.submit(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("草稿");
    }
}
