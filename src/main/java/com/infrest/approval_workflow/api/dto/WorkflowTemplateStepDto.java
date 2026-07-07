package com.infrest.approval_workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkflowTemplateStepDto(
    Long id,
    Long templateId,
    @NotNull Integer stepOrder,
    @NotBlank String approverEmail,
    String approverRole
) {}
