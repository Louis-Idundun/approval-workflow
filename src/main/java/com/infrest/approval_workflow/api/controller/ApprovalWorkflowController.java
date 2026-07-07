package com.infrest.approval_workflow.api.controller;

import com.infrest.approval_workflow.api.dto.ApprovalActionDto;
import com.infrest.approval_workflow.api.dto.ApprovalRequestResponse;
import com.infrest.approval_workflow.api.dto.CreateApprovalRequestDto;
import com.infrest.approval_workflow.service.ApprovalWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@Tag(name = "Approval Requests", description = "Approval request lifecycle endpoints")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService workflowService;

    @PostMapping
    @Operation(summary = "Submit an approval request", description = "Creates a new request with approval levels from the active workflow template")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Request created and placed IN_PROGRESS",
            content = @Content(schema = @Schema(implementation = ApprovalRequestResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or no active workflow template"),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<ApprovalRequestResponse> submitRequest(@Valid @RequestBody CreateApprovalRequestDto dto) {
        ApprovalRequestResponse response = workflowService.submitRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/action")
    @Operation(summary = "Approve or reject the current level", description = "Processes an approval action (APPROVE/REJECT) for the current level of a request")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Action processed",
            content = @Content(schema = @Schema(implementation = ApprovalRequestResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid action or request not IN_PROGRESS"),
        @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<ApprovalRequestResponse> processApprovalAction(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalActionDto dto) {
        ApprovalRequestResponse response = workflowService.processApprovalAction(id, dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request by ID", description = "Returns the full request with all approval levels")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request found",
            content = @Content(schema = @Schema(implementation = ApprovalRequestResponse.class))),
        @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<ApprovalRequestResponse> getRequest(@PathVariable Long id) {
        ApprovalRequestResponse response = workflowService.getRequest(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(summary = "Cancel a request", description = "Cancels a PENDING or IN_PROGRESS request")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Request cancelled",
            content = @Content(schema = @Schema(implementation = ApprovalRequestResponse.class))),
        @ApiResponse(responseCode = "400", description = "Request cannot be cancelled in its current state"),
        @ApiResponse(responseCode = "404", description = "Request not found")
    })
    public ResponseEntity<ApprovalRequestResponse> cancelRequest(
            @PathVariable Long id,
            @RequestParam String cancelledBy) {
        ApprovalRequestResponse response = workflowService.cancelRequest(id, cancelledBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping(params = "departmentId")
    @Operation(summary = "List requests by department", description = "Returns requests for a department, optionally filtered by status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of requests",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApprovalRequestResponse.class)))),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<List<ApprovalRequestResponse>> getRequestsByDepartment(
            @RequestParam Long departmentId,
            @RequestParam(required = false) String status) {
        List<ApprovalRequestResponse> responses = workflowService.getRequestsByDepartment(departmentId, status);
        return ResponseEntity.ok(responses);
    }

    @GetMapping(params = "approverEmail")
    @Operation(summary = "List requests pending an approver", description = "Returns all requests with a pending approval level for the given approver email")
    @ApiResponse(responseCode = "200", description = "List of pending requests",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApprovalRequestResponse.class))))
    public ResponseEntity<List<ApprovalRequestResponse>> getRequestsByApprover(
            @RequestParam String approverEmail) {
        List<ApprovalRequestResponse> responses = workflowService.getRequestsByApprover(approverEmail);
        return ResponseEntity.ok(responses);
    }
}
