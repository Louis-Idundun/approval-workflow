package com.infrest.approval_workflow.service.impl;

/**
 * Design note — Optimistic Locking with @Version
 *
 * The ApprovalRequest entity carries a @Version field ('version') that enables
 * optimistic locking at the JPA/Hibernate level. When two transactions read the
 * same request and both attempt to update it concurrently (e.g., two approvers
 * trying to action different levels at the same instant), the second transaction
 * will fail on commit because the version in the database no longer matches the
 * version the transaction read. Hibernate throws an OptimisticLockingFailureException,
 * which is caught by the GlobalExceptionHandler and surfaced to the client as an
 * HTTP 409 Conflict response with the message "The request was updated by another
 * transaction. Please retry." This guarantees that approval transitions remain
 * atomic and prevents lost updates without the overhead of pessimistic database
 * locks.
 */
import com.infrest.approval_workflow.api.dto.*;
import com.infrest.approval_workflow.api.exception.EntityNotFoundException;
import com.infrest.approval_workflow.domain.model.ApprovalLevel;
import com.infrest.approval_workflow.domain.model.ApprovalRequest;
import com.infrest.approval_workflow.domain.model.Department;
import com.infrest.approval_workflow.domain.model.WorkflowTemplate;
import com.infrest.approval_workflow.domain.enums.ApprovalLevelStatus;
import com.infrest.approval_workflow.domain.enums.RequestStatus;
import com.infrest.approval_workflow.domain.repository.*;
import com.infrest.approval_workflow.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowServiceImpl implements ApprovalWorkflowService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalLevelRepository levelRepository;
    private final DepartmentRepository departmentRepository;
    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final WorkflowTemplateStepRepository workflowTemplateStepRepository;

    @Override
    @Transactional
    public ApprovalRequestResponse submitRequest(CreateApprovalRequestDto dto) {
        Department department = departmentRepository.findById(dto.departmentId())
            .orElseThrow(() -> new EntityNotFoundException("Department", dto.departmentId()));

        WorkflowTemplate template = workflowTemplateRepository
            .findByDepartmentIdAndActiveTrue(department.getId())
            .orElseThrow(() -> new IllegalStateException("No active workflow template for department"));

        var templateSteps = workflowTemplateStepRepository
            .findByTemplateIdOrderByStepOrderAsc(template.getId());

        if (templateSteps.isEmpty()) {
            throw new IllegalStateException(
                "Workflow template '" + template.getName() + "' has no steps defined");
        }

        ApprovalRequest request = ApprovalRequest.builder()
            .title(dto.title())
            .description(dto.description())
            .submittedBy(dto.submittedBy())
            .departmentId(department.getId())
            .status(RequestStatus.PENDING)
            .currentLevel(0)
            .build();

        request = requestRepository.save(request);

        var finalRequest = request;
        List<ApprovalLevel> levels = templateSteps.stream()
            .map(step -> ApprovalLevel.builder()
                .requestId(finalRequest.getId())
                .levelNumber(step.getStepOrder())
                .approverEmail(step.getApproverEmail())
                .status(ApprovalLevelStatus.PENDING)
                .build())
            .toList();

        levelRepository.saveAll(levels);

        request.setStatus(RequestStatus.IN_PROGRESS);
        request.setCurrentLevel(1);
        request = requestRepository.save(request);

        return toResponse(request, levelRepository.findByRequestIdOrderByLevelNumberAsc(request.getId()));
    }

    @Override
    @Transactional
    public ApprovalRequestResponse processApprovalAction(Long requestId, ApprovalActionDto dto) {
        ApprovalRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("ApprovalRequest", requestId));

        if (request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Request must be IN_PROGRESS to process an approval action, but current status is " + request.getStatus());
        }

        List<ApprovalLevel> levels = levelRepository.findByRequestIdOrderByLevelNumberAsc(requestId);

        ApprovalLevel currentLevel = levels.stream()
            .filter(l -> l.getLevelNumber().equals(request.getCurrentLevel()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No approval level found for current level " + request.getCurrentLevel()));

        if (!currentLevel.getApproverEmail().equalsIgnoreCase(dto.approverEmail())) {
            throw new IllegalStateException(
                "Approver " + dto.approverEmail() + " is not assigned to level " + request.getCurrentLevel());
        }

        if (currentLevel.getStatus() != ApprovalLevelStatus.PENDING) {
            throw new IllegalStateException("Current level has already been actioned");
        }

        if (dto.action() == ApprovalActionDto.Action.APPROVE) {
            currentLevel.setStatus(ApprovalLevelStatus.APPROVED);
            currentLevel.setComment(dto.comment());
            currentLevel.setActionedAt(LocalDateTime.now());

            if (request.getCurrentLevel() >= levels.size()) {
                request.setStatus(RequestStatus.APPROVED);
            } else {
                request.setCurrentLevel(request.getCurrentLevel() + 1);
            }
        } else {
            currentLevel.setStatus(ApprovalLevelStatus.REJECTED);
            currentLevel.setComment(dto.comment());
            currentLevel.setActionedAt(LocalDateTime.now());
            request.setStatus(RequestStatus.REJECTED);
        }

        requestRepository.save(request);
        levelRepository.save(currentLevel);

        return toResponse(request, levelRepository.findByRequestIdOrderByLevelNumberAsc(requestId));
    }

    @Override
    @Transactional(readOnly = true)
    public ApprovalRequestResponse getRequest(Long requestId) {
        ApprovalRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("ApprovalRequest", requestId));

        List<ApprovalLevel> levels = levelRepository.findByRequestIdOrderByLevelNumberAsc(requestId);
        return toResponse(request, levels);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponse> getRequestsByDepartment(Long departmentId, String status) {
        departmentRepository.findById(departmentId)
            .orElseThrow(() -> new EntityNotFoundException("Department", departmentId));

        List<ApprovalRequest> requests;
        if (status != null && !status.isBlank()) {
            RequestStatus requestStatus = RequestStatus.valueOf(status.toUpperCase());
            requests = requestRepository.findByDepartmentIdAndStatus(departmentId, requestStatus);
        } else {
            requests = requestRepository.findByDepartmentId(departmentId);
        }

        return requests.stream()
            .map(req -> {
                var levels = levelRepository.findByRequestIdOrderByLevelNumberAsc(req.getId());
                return toResponse(req, levels);
            })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponse> getRequestsByApprover(String approverEmail) {
        List<ApprovalLevel> pendingLevels = levelRepository
            .findByApproverEmailAndStatus(approverEmail, ApprovalLevelStatus.PENDING);

        return pendingLevels.stream()
            .map(level -> {
                ApprovalRequest request = requestRepository.findById(level.getRequestId())
                    .orElseThrow(() -> new EntityNotFoundException("ApprovalRequest", level.getRequestId()));
                var levels = levelRepository.findByRequestIdOrderByLevelNumberAsc(request.getId());
                return toResponse(request, levels);
            })
            .toList();
    }

    @Override
    @Transactional
    public ApprovalRequestResponse cancelRequest(Long requestId, String cancelledBy) {
        ApprovalRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("ApprovalRequest", requestId));

        if (request.getStatus() != RequestStatus.PENDING && request.getStatus() != RequestStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                "Only PENDING or IN_PROGRESS requests can be cancelled, but current status is " + request.getStatus());
        }

        request.setStatus(RequestStatus.CANCELLED);
        request = requestRepository.save(request);

        return toResponse(request, levelRepository.findByRequestIdOrderByLevelNumberAsc(request.getId()));
    }

    private ApprovalRequestResponse toResponse(ApprovalRequest request, List<ApprovalLevel> levels) {
        return new ApprovalRequestResponse(
            request.getId(),
            request.getTitle(),
            request.getDescription(),
            request.getSubmittedBy(),
            request.getDepartmentId(),
            request.getStatus(),
            request.getCurrentLevel(),
            levels.stream().map(this::toLevelResponse).toList(),
            request.getCreatedAt(),
            request.getUpdatedAt()
        );
    }

    private ApprovalLevelResponse toLevelResponse(ApprovalLevel level) {
        return new ApprovalLevelResponse(
            level.getId(),
            level.getRequestId(),
            level.getLevelNumber(),
            level.getApproverEmail(),
            level.getStatus(),
            level.getComment(),
            level.getActionedAt(),
            level.getCreatedAt(),
            level.getUpdatedAt()
        );
    }
}
