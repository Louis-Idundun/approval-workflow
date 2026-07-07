package com.infrest.approval_workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkflowTemplateStepRequest(
    @NotNull Integer stepOrder,
    @NotBlank String approverEmail,
    String approverRole
) {}
