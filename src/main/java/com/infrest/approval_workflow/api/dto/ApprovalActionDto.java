package com.infrest.approval_workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApprovalActionDto(
    @NotBlank String approverEmail,
    @NotNull Action action,
    String comment
) {
    public enum Action {
        APPROVE,
        REJECT
    }
}
