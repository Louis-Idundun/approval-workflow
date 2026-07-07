package com.infrest.approval_workflow.service;

import com.infrest.approval_workflow.api.dto.*;

import java.util.List;

public interface ApprovalWorkflowService {

    ApprovalRequestResponse submitRequest(CreateApprovalRequestDto dto);

    ApprovalRequestResponse processApprovalAction(Long requestId, ApprovalActionDto dto);

    ApprovalRequestResponse getRequest(Long requestId);

    List<ApprovalRequestResponse> getRequestsByDepartment(Long departmentId, String status);

    List<ApprovalRequestResponse> getRequestsByApprover(String approverEmail);

    ApprovalRequestResponse cancelRequest(Long requestId, String cancelledBy);
}
