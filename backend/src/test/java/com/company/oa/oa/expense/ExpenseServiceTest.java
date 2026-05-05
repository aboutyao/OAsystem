package com.company.oa.oa.expense;

import com.company.oa.BaseMySqlTest;
import com.company.oa.audit.AuditService;
import com.company.oa.audit.mapper.AuditLoginLogMapper;
import com.company.oa.audit.mapper.AuditOperationLogMapper;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.oa.mapper.OaExpenseItemMapper;
import com.company.oa.oa.mapper.OaExpenseMapper;
import com.company.oa.org.mapper.UserMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import com.company.oa.workflow.WorkflowDtos;
import com.company.oa.workflow.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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

class ExpenseServiceTest extends BaseMySqlTest {

    private AuthService authService;
    private WorkflowService workflowService;
    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        workflowService = mock(WorkflowService.class);
        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        AuditService auditService = new AuditService(getMapper(AuditLoginLogMapper.class),
                getMapper(AuditOperationLogMapper.class), getMapper(SysConfigMapper.class), sequenceService);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        expenseService = new ExpenseService(
                getMapper(OaExpenseMapper.class),
                getMapper(OaExpenseItemMapper.class),
                getMapper(SysConfigMapper.class),
                getMapper(UserMapper.class),
                authService,
                workflowService,
                auditService,
                sequenceService,
                objectMapper
        );
    }

    private static AuthUser admin() {
        return new AuthUser(1L, "admin", "系统管理员", 2L, "总经办", List.of("SUPER_ADMIN"), List.of("*"));
    }

    private static List<ExpenseDtos.ExpenseItemRequest> twoItems() {
        return List.of(
                new ExpenseDtos.ExpenseItemRequest("TRAVEL", LocalDate.of(2026, 4, 1), new BigDecimal("100.00"), "交通", 1),
                new ExpenseDtos.ExpenseItemRequest("MEAL", LocalDate.of(2026, 4, 2), new BigDecimal("50.00"), null, 2)
        );
    }

    @Test
    void createSubmitWithdraw_persistsItemsAndWithdrawn() {
        when(authService.currentUser()).thenReturn(admin());
        when(workflowService.startInstance(any(WorkflowDtos.StartInstanceRequest.class))).thenReturn(Map.of(
                "processInstanceId", 100L,
                "wfInstanceId", 77L,
                "currentNodeName", "直属上级审批"
        ));

        Map<String, Object> created = expenseService.create(new ExpenseDtos.ExpenseCreateRequest(
                "TRAVEL",
                new BigDecimal("150.00"),
                "6222****",
                "出差",
                twoItems()
        ));
        long id = ((Number) created.get("id")).longValue();
        assertThat(created.get("status")).isEqualTo("DRAFT");
        assertThat(created.get("expenseNo")).isEqualTo("BX" + String.format("%012d", id));
        assertThat(created.get("items")).asList().hasSize(2);

        Map<String, Object> submitted = expenseService.submit(id);
        assertThat(submitted.get("status")).isEqualTo("APPROVING");
        verify(workflowService).startInstance(any(WorkflowDtos.StartInstanceRequest.class));

        expenseService.withdrawExpense(id);
        verify(workflowService).withdrawInstance(77L);
        Map<String, Object> after = expenseService.detail(id);
        assertThat(after.get("status")).isEqualTo("WITHDRAWN");
        assertThat(after.get("wfInstanceId")).isNull();
    }

    @Test
    void createWithMismatchedTotal_throws() {
        when(authService.currentUser()).thenReturn(admin());
        assertThatThrownBy(() -> expenseService.create(new ExpenseDtos.ExpenseCreateRequest(
                "TRAVEL",
                new BigDecimal("999.00"),
                null,
                null,
                twoItems()
        ))).isInstanceOf(BusinessException.class).hasMessageContaining("合计");
    }

    @Test
    void cancelDraft_doesNotCallTerminate() {
        when(authService.currentUser()).thenReturn(admin());
        long id = ((Number) expenseService.create(new ExpenseDtos.ExpenseCreateRequest(
                "OFFICE",
                new BigDecimal("150.00"),
                null,
                null,
                twoItems()
        )).get("id")).longValue();
        expenseService.cancelExpense(id);
        verify(workflowService, never()).terminateInstance(anyLong());
        assertThat(expenseService.detail(id).get("status")).isEqualTo("CANCELLED");
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    @Test
    void markPaid_whenApproved_setsPaid() {
        when(authService.currentUser()).thenReturn(admin());
        when(workflowService.startInstance(any(WorkflowDtos.StartInstanceRequest.class))).thenReturn(Map.of(
                "processInstanceId", 101L,
                "wfInstanceId", 1L,
                "currentNodeName", "审批"
        ));
        long id = ((Number) expenseService.create(new ExpenseDtos.ExpenseCreateRequest(
                "TRAVEL",
                new BigDecimal("150.00"),
                null,
                null,
                twoItems()
        )).get("id")).longValue();
        expenseService.submit(id);
        jdbc.update("update oa_expense set status = 'APPROVED' where id = ?", id);

        Map<String, Object> paid = expenseService.markPaid(id, new ExpenseDtos.ExpenseMarkPaidRequest(new BigDecimal("150.00")));
        assertThat(paid.get("paymentStatus")).isEqualTo("PAID");
        assertThat(toBigDecimal(paid.get("paidAmount"))).isEqualByComparingTo("150");

        Long opCount = jdbc.queryForObject(
                "select count(*) from audit_operation_log where business_type = ? and business_id = ? and operation_type = ?",
                Long.class, "EXPENSE", id, "EXPENSE_MARK_PAID");
        assertThat(opCount).isEqualTo(1L);
        Long submitCount = jdbc.queryForObject(
                "select count(*) from audit_operation_log where business_type = ? and business_id = ? and operation_type = ?",
                Long.class, "EXPENSE", id, "EXPENSE_SUBMIT");
        assertThat(submitCount).isEqualTo(1L);
    }
}
