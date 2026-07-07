package com.infrest.approval_workflow.api.controller;

import com.infrest.approval_workflow.api.dto.*;
import com.infrest.approval_workflow.api.exception.GlobalExceptionHandler;
import com.infrest.approval_workflow.api.exception.EntityNotFoundException;
import com.infrest.approval_workflow.domain.enums.RequestStatus;
import com.infrest.approval_workflow.service.ApprovalWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApprovalWorkflowController.class)
@Import(GlobalExceptionHandler.class)
class ApprovalWorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApprovalWorkflowService workflowService;

    @Test
    void submitRequest_withValidBody_shouldReturn201() throws Exception {
        ApprovalRequestResponse response = new ApprovalRequestResponse(
            1L, "Test", null, "emp@test.com", 1L,
            RequestStatus.IN_PROGRESS, 1, List.of(), null, null);

        when(workflowService.submitRequest(any(CreateApprovalRequestDto.class))).thenReturn(response);

        String json = """
            {
                "title": "Test Request",
                "description": "Please approve",
                "submittedBy": "emp@test.com",
                "departmentId": 1
            }
            """;

        mockMvc.perform(post("/api/v1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Test"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void submitRequest_withMissingTitle_shouldReturn400() throws Exception {
        String json = """
            {
                "description": "Please approve",
                "submittedBy": "emp@test.com",
                "departmentId": 1
            }
            """;

        mockMvc.perform(post("/api/v1/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void processApprovalAction_onNonExistentRequest_shouldReturn404() throws Exception {
        when(workflowService.processApprovalAction(eq(999L), any(ApprovalActionDto.class)))
            .thenThrow(new EntityNotFoundException("ApprovalRequest", 999L));

        String json = """
            {
                "approverEmail": "lead@test.com",
                "action": "APPROVE",
                "comment": "OK"
            }
            """;

        mockMvc.perform(post("/api/v1/requests/999/action")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("ApprovalRequest with id 999 not found"));
    }

    @Test
    void getRequest_byId_shouldReturn200() throws Exception {
        ApprovalRequestResponse response = new ApprovalRequestResponse(
            1L, "Test", "Desc", "emp@test.com", 1L,
            RequestStatus.IN_PROGRESS, 1,
            List.of(new ApprovalLevelResponse(10L, 1L, 1, "lead@test.com",
                com.infrest.approval_workflow.domain.enums.ApprovalLevelStatus.PENDING,
                null, null, null, null)),
            null, null);

        when(workflowService.getRequest(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/requests/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test"))
            .andExpect(jsonPath("$.approvalLevels").isArray())
            .andExpect(jsonPath("$.approvalLevels[0].approverEmail").value("lead@test.com"));
    }
}
