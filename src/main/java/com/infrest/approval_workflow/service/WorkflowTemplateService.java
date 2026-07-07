package com.infrest.approval_workflow.service;

import com.infrest.approval_workflow.api.dto.CreateWorkflowTemplateRequest;
import com.infrest.approval_workflow.api.dto.WorkflowTemplateDto;

import java.util.List;

public interface WorkflowTemplateService {

    WorkflowTemplateDto createTemplate(Long departmentId, CreateWorkflowTemplateRequest request);

    List<WorkflowTemplateDto> getTemplatesByDepartment(Long departmentId);

    WorkflowTemplateDto activateTemplate(Long departmentId, Long templateId);
}
