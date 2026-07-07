package com.infrest.approval_workflow.api.dto;

import com.infrest.approval_workflow.domain.enums.ApprovalLevelStatus;

import java.time.LocalDateTime;

public record ApprovalLevelResponse(
    Long id,
    Long requestId,
    Integer levelNumber,
    String approverEmail,
    ApprovalLevelStatus status,
    String comment,
    LocalDateTime actionedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
