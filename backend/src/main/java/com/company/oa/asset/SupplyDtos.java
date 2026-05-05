package com.company.oa.asset;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public final class SupplyDtos {
    private SupplyDtos() {
    }

    public record SupplyCreateRequest(
            @NotBlank String supplyCode,
            @NotBlank String supplyName,
            String category,
            @NotBlank String unit,
            BigDecimal warningQuantity,
            String remark
    ) {
    }

    public record SupplyUpdateRequest(
            @NotBlank String supplyName,
            String category,
            @NotBlank String unit,
            BigDecimal warningQuantity,
            @NotBlank String status,
            String remark
    ) {
    }

    public record SupplyMovementRequest(
            @NotNull @DecimalMin(value = "0.01", message = "数量必须大于 0") BigDecimal quantity,
            Long userId,
            String reason
    ) {
    }

    public record SupplyAdjustRequest(
            @NotNull @DecimalMin(value = "0", message = "目标库存须 >= 0") BigDecimal quantity,
            String reason
    ) {
    }
}
