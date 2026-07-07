package com.infrest.approval_workflow.service.impl;

import com.infrest.approval_workflow.api.dto.CreateWorkflowTemplateRequest;
import com.infrest.approval_workflow.api.dto.WorkflowTemplateDto;
import com.infrest.approval_workflow.api.dto.WorkflowTemplateStepDto;
import com.infrest.approval_workflow.api.exception.EntityNotFoundException;
import com.infrest.approval_workflow.domain.model.WorkflowTemplate;
import com.infrest.approval_workflow.domain.model.WorkflowTemplateStep;
import com.infrest.approval_workflow.domain.repository.DepartmentRepository;
import com.infrest.approval_workflow.domain.repository.WorkflowTemplateRepository;
import com.infrest.approval_workflow.domain.repository.WorkflowTemplateStepRepository;
import com.infrest.approval_workflow.service.WorkflowTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowTemplateServiceImpl implements WorkflowTemplateService {

    private final DepartmentRepository departmentRepository;
    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowTemplateStepRepository templateStepRepository;

    @Override
    @Transactional
    public WorkflowTemplateDto createTemplate(Long departmentId, CreateWorkflowTemplateRequest request) {
        departmentRepository.findById(departmentId)
            .orElseThrow(() -> new EntityNotFoundException("Department", departmentId));

        WorkflowTemplate template = WorkflowTemplate.builder()
            .departmentId(departmentId)
            .name(request.name())
            .active(false)
            .build();

        template = templateRepository.save(template);

        var finalTemplate = template;
        List<WorkflowTemplateStep> steps = request.steps().stream()
            .map(step -> WorkflowTemplateStep.builder()
                .templateId(finalTemplate.getId())
                .stepOrder(step.stepOrder())
                .approverEmail(step.approverEmail())
                .approverRole(step.approverRole())
                .build())
            .toList();

        templateStepRepository.saveAll(steps);

        return toDto(template, steps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowTemplateDto> getTemplatesByDepartment(Long departmentId) {
        departmentRepository.findById(departmentId)
            .orElseThrow(() -> new EntityNotFoundException("Department", departmentId));

        return templateRepository.findByDepartmentId(departmentId).stream()
            .map(template -> {
                var steps = templateStepRepository.findByTemplateIdOrderByStepOrderAsc(template.getId());
                return toDto(template, steps);
            })
            .toList();
    }

    @Override
    @Transactional
    public WorkflowTemplateDto activateTemplate(Long departmentId, Long templateId) {
        departmentRepository.findById(departmentId)
            .orElseThrow(() -> new EntityNotFoundException("Department", departmentId));

        templateRepository.findById(templateId)
            .orElseThrow(() -> new EntityNotFoundException("WorkflowTemplate", templateId));

        List<WorkflowTemplate> templates = templateRepository.findByDepartmentId(departmentId);
        templates.forEach(t -> t.setActive(t.getId().equals(templateId)));
        templateRepository.saveAll(templates);

        WorkflowTemplate activated = templates.stream()
            .filter(t -> t.getId().equals(templateId))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("WorkflowTemplate", templateId));

        var steps = templateStepRepository.findByTemplateIdOrderByStepOrderAsc(activated.getId());
        return toDto(activated, steps);
    }

    private WorkflowTemplateDto toDto(WorkflowTemplate template, List<WorkflowTemplateStep> steps) {
        return new WorkflowTemplateDto(
            template.getId(),
            template.getDepartmentId(),
            template.getName(),
            template.getActive(),
            steps.stream()
                .map(s -> new WorkflowTemplateStepDto(
                    s.getId(),
                    s.getTemplateId(),
                    s.getStepOrder(),
                    s.getApproverEmail(),
                    s.getApproverRole()))
                .toList()
        );
    }
}
