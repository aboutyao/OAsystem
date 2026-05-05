package com.company.oa.oa.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class PurchaseDtos {
    private PurchaseDtos() {
    }

    public record PurchaseItemRequest(
            @NotBlank String itemName,
            String specification,
            @NotNull BigDecimal quantity,
            String unit,
            @NotNull BigDecimal unitPrice,
            @NotNull BigDecimal amount,
            @NotNull Integer sortOrder
    ) {
    }

    public record PurchaseCreateRequest(
            @NotBlank String purchaseType,
            @NotNull BigDecimal totalAmount,
            String supplierName,
            String budgetSubject,
            String reason,
            @NotEmpty @Valid List<PurchaseItemRequest> items
    ) {
    }

    public record PurchaseUpdateRequest(
            @NotBlank String purchaseType,
            @NotNull BigDecimal totalAmount,
            String supplierName,
            String budgetSubject,
            String reason,
            @NotEmpty @Valid List<PurchaseItemRequest> items
    ) {
    }
}
