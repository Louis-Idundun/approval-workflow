package com.infrest.approval_workflow.service.impl;

import com.infrest.approval_workflow.api.dto.DepartmentDto;
import com.infrest.approval_workflow.api.exception.EntityNotFoundException;
import com.infrest.approval_workflow.domain.model.Department;
import com.infrest.approval_workflow.domain.repository.DepartmentRepository;
import com.infrest.approval_workflow.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public DepartmentDto createDepartment(DepartmentDto dto) {
        if (departmentRepository.existsByCode(dto.code())) {
            throw new IllegalArgumentException("Department with code '" + dto.code() + "' already exists");
        }

        Department department = Department.builder()
            .name(dto.name())
            .code(dto.code())
            .build();

        department = departmentRepository.save(department);
        return toDto(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDto getDepartment(Long id) {
        Department department = departmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Department", id));
        return toDto(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }

    private DepartmentDto toDto(Department department) {
        return new DepartmentDto(department.getId(), department.getName(), department.getCode());
    }
}
