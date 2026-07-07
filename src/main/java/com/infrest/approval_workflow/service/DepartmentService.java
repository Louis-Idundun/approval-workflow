package com.infrest.approval_workflow.service;

import com.infrest.approval_workflow.api.dto.DepartmentDto;

import java.util.List;

public interface DepartmentService {

    DepartmentDto createDepartment(DepartmentDto dto);

    DepartmentDto getDepartment(Long id);

    List<DepartmentDto> getAllDepartments();
}
