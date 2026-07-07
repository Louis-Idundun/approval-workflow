package com.infrest.approval_workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record WorkflowTemplateDto(
    Long id,
    @NotNull Long departmentId,
    @NotBlank String name,
    Boolean active,
    List<WorkflowTemplateStepDto> steps
) {}
