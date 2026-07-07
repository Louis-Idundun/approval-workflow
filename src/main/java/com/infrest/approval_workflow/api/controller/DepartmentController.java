package com.infrest.approval_workflow.api.controller;

import com.infrest.approval_workflow.api.dto.DepartmentDto;
import com.infrest.approval_workflow.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department management endpoints")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @Operation(summary = "Create a department", description = "Creates a new department with a unique code")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Department created",
            content = @Content(schema = @Schema(implementation = DepartmentDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or duplicate code")
    })
    public ResponseEntity<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentDto dto) {
        DepartmentDto response = departmentService.createDepartment(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all departments")
    @ApiResponse(responseCode = "200", description = "List of departments",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = DepartmentDto.class))))
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        List<DepartmentDto> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Department found",
            content = @Content(schema = @Schema(implementation = DepartmentDto.class))),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<DepartmentDto> getDepartment(@PathVariable Long id) {
        DepartmentDto response = departmentService.getDepartment(id);
        return ResponseEntity.ok(response);
    }
}
