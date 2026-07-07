package com.infrest.approval_workflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentDto(
    Long id,
    @NotBlank String name,
    @NotBlank @Size(max = 50) String code
) {}
