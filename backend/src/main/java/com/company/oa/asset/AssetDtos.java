package com.company.oa.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class AssetDtos {
    private AssetDtos() {
    }

    public record AssetCreateRequest(
            @NotBlank String assetNo,
            @NotBlank String assetName,
            String assetCategory,
            String model,
            LocalDate purchaseDate,
            BigDecimal purchaseAmount,
            Long responsibleUserId,
            Long deptId,
            String remark
    ) {
    }

    public record AssetUpdateRequest(
            @NotBlank String assetName,
            String assetCategory,
            String model,
            LocalDate purchaseDate,
            BigDecimal purchaseAmount,
            Long deptId,
            String remark
    ) {
    }

    public record AssetActionRequest(
            @NotNull Long targetUserId,
            String reason
    ) {
        public AssetActionRequest {
        }

        public static AssetActionRequest empty() {
            return new AssetActionRequest(null, null);
        }
    }

    public record AssetReasonRequest(String reason) {
    }
}
