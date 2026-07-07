package com.infrest.approval_workflow.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateWorkflowTemplateRequest(
    @NotBlank String name,
    @NotEmpty @Valid List<CreateWorkflowTemplateStepRequest> steps
) {}
