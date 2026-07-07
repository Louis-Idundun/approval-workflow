package com.infrest.approval_workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateApprovalRequestDto(
    @NotBlank String title,
    String description,
    @NotBlank String submittedBy,
    @NotNull Long departmentId
) {}
