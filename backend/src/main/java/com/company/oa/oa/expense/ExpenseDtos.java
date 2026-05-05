package com.company.oa.oa.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ExpenseDtos {
    private ExpenseDtos() {
    }

    public record ExpenseItemRequest(
            @NotBlank String feeType,
            @NotNull LocalDate feeDate,
            @NotNull BigDecimal amount,
            String description,
            @NotNull Integer sortOrder
    ) {
    }

    public record ExpenseCreateRequest(
            @NotBlank String expenseType,
            @NotNull BigDecimal totalAmount,
            String payeeAccount,
            String reason,
            @NotEmpty @Valid List<ExpenseItemRequest> items
    ) {
    }

    public record ExpenseUpdateRequest(
            @NotBlank String expenseType,
            @NotNull BigDecimal totalAmount,
            String payeeAccount,
            String reason,
            @NotEmpty @Valid List<ExpenseItemRequest> items
    ) {
    }

    public record ExpenseMarkPaidRequest(BigDecimal paidAmount) {
    }
}
