package com.company.oa.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ContractDtos {
    private ContractDtos() {
    }

    public record ContractCreateRequest(
            @NotBlank String contractName,
            @NotBlank String contractType,
            @NotBlank String counterparty,
            @NotNull BigDecimal amount,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record ContractUpdateRequest(
            @NotBlank String contractName,
            @NotBlank String contractType,
            @NotBlank String counterparty,
            @NotNull BigDecimal amount,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
