package com.company.oa.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class WorkflowDtos {
    private WorkflowDtos() {
    }

    /** variables 可为 null；审批人未配置时须包含 managerId（Long） */
    public record StartInstanceRequest(
            @NotBlank String businessType,
            @NotNull Long businessId,
            @NotBlank String title,
            Map<String, Object> variables
    ) {
        public StartInstanceRequest {
            variables = variables == null ? Map.of() : variables;
        }
    }

    public record ApproveRequest(String comment, List<Long> attachmentIds) {
    }

    public record RejectRequest(String comment, String rejectTo) {
    }

    public record TransferRequest(@NotNull Long targetUserId, String comment) {
    }

    public record AddSignRequest(
            @NotNull Long assigneeUserId,
            String mode,
            String comment
    ) {
    }

    public record DelegateCreateRequest(
            @NotNull Long delegateeId,
            @NotNull OffsetDateTime startAt,
            @NotNull OffsetDateTime endAt,
            String businessScope,
            String reason
    ) {
    }

    public record CcAddRequest(
            @NotNull Long wfInstanceId,
            @NotNull List<Long> receiverIds,
            String reason
    ) {
    }

    public record CreateTemplateRequest(
            @NotBlank String templateCode,
            @NotBlank String templateName,
            @NotBlank String businessType,
            String description
    ) {
    }

    public record UpdateTemplateRequest(
            String templateName,
            String description,
            String status
    ) {
    }

    public record CreateVersionRequest(
            String changeReason
    ) {
    }
}
