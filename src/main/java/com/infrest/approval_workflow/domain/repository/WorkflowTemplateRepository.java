package com.infrest.approval_workflow.domain.repository;

import com.infrest.approval_workflow.domain.model.WorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, Long> {

    List<WorkflowTemplate> findByDepartmentId(Long departmentId);

    Optional<WorkflowTemplate> findByDepartmentIdAndActiveTrue(Long departmentId);

    List<WorkflowTemplate> findByDepartmentIdAndActiveTrueOrderByName(Long departmentId);
}
