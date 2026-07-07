package com.infrest.approval_workflow.api.dto;

import com.infrest.approval_workflow.domain.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApprovalRequestResponse(
    Long id,
    String title,
    String description,
    String submittedBy,
    Long departmentId,
    RequestStatus status,
    Integer currentLevel,
    List<ApprovalLevelResponse> approvalLevels,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
