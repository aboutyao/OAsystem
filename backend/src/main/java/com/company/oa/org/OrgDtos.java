package com.company.oa.org;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class OrgDtos {
    private OrgDtos() {
    }

    public record DeptCreateRequest(
            @NotBlank @Size(max = 64) String deptCode,
            @NotBlank @Size(max = 128) String deptName,
            Long parentId,
            Long leaderUserId,
            Integer sortOrder
    ) {
    }

    public record DeptUpdateRequest(
            @NotBlank @Size(max = 64) String deptCode,
            @NotBlank @Size(max = 128) String deptName,
            Long parentId,
            Long leaderUserId,
            Integer sortOrder
    ) {
    }

    public record UserCreateRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 64) String employeeNo,
            @NotBlank @Size(max = 64) String realName,
            @Size(max = 32) String mobile,
            @Size(max = 128) String email,
            @NotNull Long mainDeptId,
            Long positionId,
            Long rankId,
            Long managerUserId,
            String password,
            List<Long> roleIds
    ) {
    }

    public record UserUpdateRequest(
            @NotBlank @Size(max = 64) String employeeNo,
            @NotBlank @Size(max = 64) String realName,
            @Size(max = 32) String mobile,
            @Size(max = 128) String email,
            @NotNull Long mainDeptId,
            Long positionId,
            Long rankId,
            Long managerUserId,
            /** null 表示不修改角色绑定 */
            List<Long> roleIds
    ) {
    }

    public record PositionUpsertRequest(
            @NotBlank @Size(max = 64) String positionCode,
            @NotBlank @Size(max = 128) String positionName,
            String status,
            Integer sortOrder,
            String remark
    ) {
    }

    public record RankUpsertRequest(
            @NotBlank @Size(max = 64) String rankCode,
            @NotBlank @Size(max = 128) String rankName,
            @NotNull Integer rankLevel,
            String status,
            String remark
    ) {
    }

    public record UserImportResult(int created, int skipped, List<String> errors) {
    }
}
