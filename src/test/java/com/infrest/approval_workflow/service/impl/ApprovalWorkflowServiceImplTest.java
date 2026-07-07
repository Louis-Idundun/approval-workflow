package com.infrest.approval_workflow.service.impl;

import com.infrest.approval_workflow.api.dto.ApprovalActionDto;
import com.infrest.approval_workflow.api.dto.ApprovalLevelResponse;
import com.infrest.approval_workflow.api.dto.ApprovalRequestResponse;
import com.infrest.approval_workflow.api.dto.CreateApprovalRequestDto;
import com.infrest.approval_workflow.api.exception.EntityNotFoundException;
import com.infrest.approval_workflow.domain.enums.ApprovalLevelStatus;
import com.infrest.approval_workflow.domain.enums.RequestStatus;
import com.infrest.approval_workflow.domain.model.ApprovalLevel;
import com.infrest.approval_workflow.domain.model.ApprovalRequest;
import com.infrest.approval_workflow.domain.model.Department;
import com.infrest.approval_workflow.domain.model.WorkflowTemplate;
import com.infrest.approval_workflow.domain.model.WorkflowTemplateStep;
import com.infrest.approval_workflow.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceImplTest {

    @Mock private ApprovalRequestRepository requestRepository;
    @Mock private ApprovalLevelRepository levelRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private WorkflowTemplateRepository workflowTemplateRepository;
    @Mock private WorkflowTemplateStepRepository workflowTemplateStepRepository;
    @Captor private ArgumentCaptor<ApprovalRequest> requestCaptor;
    @Captor private ArgumentCaptor<ApprovalLevel> levelCaptor;

    private ApprovalWorkflowServiceImpl service;

    private Department department;
    private WorkflowTemplate template;
    private List<WorkflowTemplateStep> templateSteps;
    private ApprovalRequest savedRequest;

    @BeforeEach
    void setUp() {
        service = new ApprovalWorkflowServiceImpl(
            requestRepository, levelRepository, departmentRepository,
            workflowTemplateRepository, workflowTemplateStepRepository);

        department = Department.builder().id(1L).name("Engineering").code("ENG").build();

        template = WorkflowTemplate.builder().id(10L).departmentId(1L).name("Test Template").active(true).build();

        templateSteps = List.of(
            WorkflowTemplateStep.builder().id(100L).templateId(10L).stepOrder(1).approverEmail("lead@test.com").approverRole("Lead").build(),
            WorkflowTemplateStep.builder().id(101L).templateId(10L).stepOrder(2).approverEmail("manager@test.com").approverRole("Manager").build());

        savedRequest = ApprovalRequest.builder()
            .id(1000L).title("Test").description("Desc")
            .submittedBy("emp@test.com").departmentId(1L)
            .status(RequestStatus.PENDING).currentLevel(0).version(0)
            .build();
    }

    // ---------------------------------------------------------------
    // submitRequest
    // ---------------------------------------------------------------

    @Test
    void submitRequest_shouldCreateRequestWithLevels_whenValidTemplate() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(workflowTemplateRepository.findByDepartmentIdAndActiveTrue(1L)).thenReturn(Optional.of(template));
        when(workflowTemplateStepRepository.findByTemplateIdOrderByStepOrderAsc(10L)).thenReturn(templateSteps);

        when(requestRepository.save(any(ApprovalRequest.class)))
            .thenReturn(savedRequest)
            .thenReturn(savedRequest); // second call after status update

        List<ApprovalLevel> createdLevels = List.of(
            ApprovalLevel.builder().id(200L).requestId(1000L).levelNumber(1).approverEmail("lead@test.com").status(ApprovalLevelStatus.PENDING).build(),
            ApprovalLevel.builder().id(201L).requestId(1000L).levelNumber(2).approverEmail("manager@test.com").status(ApprovalLevelStatus.PENDING).build());
        when(levelRepository.saveAll(anyList())).thenReturn(createdLevels);
        when(levelRepository.findByRequestIdOrderByLevelNumberAsc(1000L)).thenReturn(createdLevels);

        CreateApprovalRequestDto dto = new CreateApprovalRequestDto("Test Request", "Some desc", "emp@test.com", 1L);

        ApprovalRequestResponse response = service.submitRequest(dto);

        assertThat(response.title()).isEqualTo("Test");
        assertThat(response.status()).isEqualTo(RequestStatus.IN_PROGRESS);
        assertThat(response.currentLevel()).isEqualTo(1);
        assertThat(response.approvalLevels()).hasSize(2);

        verify(requestRepository, times(2)).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
        assertThat(requestCaptor.getValue().getCurrentLevel()).isEqualTo(1);
        verify(levelRepository).saveAll(anyList());
    }

    @Test
    void submitRequest_shouldThrowException_whenNoDepartment() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        CreateApprovalRequestDto dto = new CreateApprovalRequestDto("X", null, "emp@test.com", 99L);

        assertThatThrownBy(() -> service.submitRequest(dto))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Department");

        verifyNoInteractions(workflowTemplateRepository);
    }

    @Test
    void submitRequest_shouldThrowException_whenNoActiveTemplate() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(workflowTemplateRepository.findByDepartmentIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        CreateApprovalRequestDto dto = new CreateApprovalRequestDto("X", null, "emp@test.com", 1L);

        assertThatThrownBy(() -> service.submitRequest(dto))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No active workflow template");
    }

    // ---------------------------------------------------------------
    // processApprovalAction
    // ---------------------------------------------------------------

    @Test
    void processApprovalAction_shouldApproveLevel_andIncrementCurrentLevel() {
        ApprovalRequest request = ApprovalRequest.builder()
            .id(1000L).title("Test").submittedBy("emp@test.com").departmentId(1L)
            .status(RequestStatus.IN_PROGRESS).currentLevel(1).version(0)
            .build();

        List<ApprovalLevel> levels = List.of(
            ApprovalLevel.builder().id(200L).requestId(1000L).levelNumber(1).approverEmail("lead@test.com").status(ApprovalLevelStatus.PENDING).build(),
            ApprovalLevel.builder().id(201L).requestId(1000L).levelNumber(2).approverEmail("manager@test.com").status(ApprovalLevelStatus.PENDING).build(),
            ApprovalLevel.builder().id(202L).requestId(1000L).levelNumber(3).approverEmail("cto@test.com").status(ApprovalLevelStatus.PENDING).build());

        when(requestRepository.findById(1000L)).thenReturn(Optional.of(request));
        when(levelRepository.findByRequestIdOrderByLevelNumberAsc(1000L)).thenReturn(levels);

        ApprovalActionDto dto = new ApprovalActionDto("lead@test.com", ApprovalActionDto.Action.APPROVE, "Looks good");

        service.processApprovalAction(1000L, dto);

        ArgumentCaptor<ApprovalLevel> levelCaptor = ArgumentCaptor.captor();
        verify(levelRepository).save(levelCaptor.capture());
        assertThat(levelCaptor.getValue().getStatus()).isEqualTo(ApprovalLevelStatus.APPROVED);
        assertThat(levelCaptor.getValue().getComment()).isEqualTo("Looks good");
        assertThat(levelCaptor.getValue().getActionedAt()).isNotNull();

        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getCurrentLevel()).isEqualTo(2);
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
    }

    @Test
    void processApprovalAction_shouldMarkRequestApproved_whenLastLevelApproved() {
        ApprovalRequest request = ApprovalRequest.builder()
            .id(1000L).title("Test").submittedBy("emp@test.com").departmentId(1L)
            .status(RequestStatus.IN_PROGRESS).currentLevel(3).version(0)
            .build();

        List<ApprovalLevel> levels = List.of(
            ApprovalLevel.builder().id(200L).requestId(1000L).levelNumber(1).approverEmail("lead@test.com").status(ApprovalLevelStatus.APPROVED).build(),
            ApprovalLevel.builder().id(201L).requestId(1000L).levelNumber(2).approverEmail("manager@test.com").status(ApprovalLevelStatus.APPROVED).build(),
            ApprovalLevel.builder().id(202L).requestId(1000L).levelNumber(3).approverEmail("cto@test.com").status(ApprovalLevelStatus.PENDING).build());

        when(requestRepository.findById(1000L)).thenReturn(Optional.of(request));
        when(levelRepository.findByRequestIdOrderByLevelNumberAsc(1000L)).thenReturn(levels);

        ApprovalActionDto dto = new ApprovalActionDto("cto@test.com", ApprovalActionDto.Action.APPROVE, "Approved");

        service.processApprovalAction(1000L, dto);

        verify(levelRepository).save(any(ApprovalLevel.class));
        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(requestCaptor.getValue().getCurrentLevel()).isEqualTo(3);
    }

    @Test
    void processApprovalAction_shouldMarkRequestRejected_whenLevelRejected() {
        ApprovalRequest request = ApprovalRequest.builder()
            .id(1000L).title("Test").submittedBy("emp@test.com").departmentId(1L)
            .status(RequestStatus.IN_PROGRESS).currentLevel(1).version(0)
            .build();

        List<ApprovalLevel> levels = List.of(
            ApprovalLevel.builder().id(200L).requestId(1000L).levelNumber(1).approverEmail("lead@test.com").status(ApprovalLevelStatus.PENDING).build(),
            ApprovalLevel.builder().id(201L).requestId(1000L).levelNumber(2).approverEmail("manager@test.com").status(ApprovalLevelStatus.PENDING).build());

        when(requestRepository.findById(1000L)).thenReturn(Optional.of(request));
        when(levelRepository.findByRequestIdOrderByLevelNumberAsc(1000L)).thenReturn(levels);

        ApprovalActionDto dto = new ApprovalActionDto("lead@test.com", ApprovalActionDto.Action.REJECT, "Not ready");

        service.processApprovalAction(1000L, dto);

        verify(levelRepository).save(levelCaptor.capture());
        assertThat(levelCaptor.getValue().getStatus()).isEqualTo(ApprovalLevelStatus.REJECTED);

        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void processApprovalAction_shouldThrowException_whenRequestNotInProgress() {
        ApprovalRequest request = ApprovalRequest.builder()
            .id(1000L).title("Test").submittedBy("emp@test.com").departmentId(1L)
            .status(RequestStatus.PENDING).currentLevel(0).version(0)
            .build();

        when(requestRepository.findById(1000L)).thenReturn(Optional.of(request));

        ApprovalActionDto dto = new ApprovalActionDto("lead@test.com", ApprovalActionDto.Action.APPROVE, null);

        assertThatThrownBy(() -> service.processApprovalAction(1000L, dto))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be IN_PROGRESS")
            .hasMessageContaining("PENDING");
    }

    // ---------------------------------------------------------------
    // cancelRequest
    // ---------------------------------------------------------------

    @Test
    void cancelRequest_shouldSetStatusCancelled() {
        ApprovalRequest request = ApprovalRequest.builder()
            .id(1000L).title("Test").submittedBy("emp@test.com").departmentId(1L)
            .status(RequestStatus.IN_PROGRESS).currentLevel(1).version(0)
            .build();

        when(requestRepository.findById(1000L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ApprovalRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(levelRepository.findByRequestIdOrderByLevelNumberAsc(1000L)).thenReturn(List.of());

        service.cancelRequest(1000L, "emp@test.com");

        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(RequestStatus.CANCELLED);
    }

    @Test
    void cancelRequest_shouldThrowException_whenAlreadyApproved() {
        ApprovalRequest request = ApprovalRequest.builder()
            .id(1000L).title("Test").submittedBy("emp@test.com").departmentId(1L)
            .status(RequestStatus.APPROVED).currentLevel(3).version(0)
            .build();

        when(requestRepository.findById(1000L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.cancelRequest(1000L, "emp@test.com"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PENDING or IN_PROGRESS");
    }
}
