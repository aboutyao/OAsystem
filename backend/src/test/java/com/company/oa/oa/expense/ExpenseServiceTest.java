package com.company.oa.oa.expense;

import com.company.oa.BaseSpringTest;
import com.company.oa.common.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseServiceTest extends BaseSpringTest {

    @Autowired
    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        // Clear expense-related data to avoid test pollution
        jdbc.update("DELETE FROM oa_expense_item WHERE expense_id IN (SELECT id FROM oa_expense WHERE expense_no LIKE 'TEST_%')");
        jdbc.update("DELETE FROM oa_expense WHERE expense_no LIKE 'TEST_%'");
    }

    private static List<ExpenseDtos.ExpenseItemRequest> twoItems() {
        return List.of(
                new ExpenseDtos.ExpenseItemRequest("TRAVEL", LocalDate.of(2026, 4, 1), new BigDecimal("100.00"), "交通", 1),
                new ExpenseDtos.ExpenseItemRequest("MEAL", LocalDate.of(2026, 4, 2), new BigDecimal("50.00"), null, 2)
        );
    }

    @Test
    void createSubmitWithdraw_persistsItemsAndWithdrawn() {
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

        expenseService.withdrawExpense(id);
        Map<String, Object> after = expenseService.detail(id);
        assertThat(after.get("status")).isEqualTo("WITHDRAWN");
        assertThat(after.get("wfInstanceId")).isNull();
    }

    @Test
    void createWithMismatchedTotal_throws() {
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
        long id = ((Number) expenseService.create(new ExpenseDtos.ExpenseCreateRequest(
                "OFFICE",
                new BigDecimal("150.00"),
                null,
                null,
                twoItems()
        )).get("id")).longValue();
        expenseService.cancelExpense(id);
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
        Map<String, Object> created = expenseService.create(new ExpenseDtos.ExpenseCreateRequest(
                "TRAVEL",
                new BigDecimal("150.00"),
                null,
                null,
                twoItems()
        ));
        long id = ((Number) created.get("id")).longValue();
        expenseService.submit(id);
        jdbc.update("update oa_expense set status = 'APPROVED' where id = ?", id);

        Map<String, Object> paid = expenseService.markPaid(id, new ExpenseDtos.ExpenseMarkPaidRequest(new BigDecimal("150.00")));
        assertThat(paid.get("paymentStatus")).isEqualTo("PAID");
        assertThat(toBigDecimal(paid.get("paidAmount"))).isEqualByComparingTo("150");

        Long opCount = jdbc.queryForObject(
                "select count(*) from audit_operation_log where business_type = ? and business_id = ? and operation_type = ?",
                Long.class, "EXPENSE", id, "UPDATE");
        assertThat(opCount).isGreaterThanOrEqualTo(1L);
    }
}
