package com.infrest.approval_workflow.api.controller;

import com.infrest.approval_workflow.api.dto.CreateWorkflowTemplateRequest;
import com.infrest.approval_workflow.api.dto.WorkflowTemplateDto;
import com.infrest.approval_workflow.service.WorkflowTemplateService;
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
@RequestMapping("/api/v1/departments/{departmentId}/templates")
@RequiredArgsConstructor
@Tag(name = "Workflow Templates", description = "Workflow template management per department")
public class WorkflowTemplateController {

    private final WorkflowTemplateService templateService;

    @PostMapping
    @Operation(summary = "Create a workflow template", description = "Creates a template with approval steps for a department")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Template created",
            content = @Content(schema = @Schema(implementation = WorkflowTemplateDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<WorkflowTemplateDto> createTemplate(
            @PathVariable Long departmentId,
            @Valid @RequestBody CreateWorkflowTemplateRequest request) {
        WorkflowTemplateDto response = templateService.createTemplate(departmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List templates for a department")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of templates",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = WorkflowTemplateDto.class)))),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<List<WorkflowTemplateDto>> getTemplates(@PathVariable Long departmentId) {
        List<WorkflowTemplateDto> templates = templateService.getTemplatesByDepartment(departmentId);
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/{templateId}/activate")
    @Operation(summary = "Activate a template", description = "Sets the given template as active and deactivates all others for the department")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template activated",
            content = @Content(schema = @Schema(implementation = WorkflowTemplateDto.class))),
        @ApiResponse(responseCode = "404", description = "Department or template not found")
    })
    public ResponseEntity<WorkflowTemplateDto> activateTemplate(
            @PathVariable Long departmentId,
            @PathVariable Long templateId) {
        WorkflowTemplateDto response = templateService.activateTemplate(departmentId, templateId);
        return ResponseEntity.ok(response);
    }
}
