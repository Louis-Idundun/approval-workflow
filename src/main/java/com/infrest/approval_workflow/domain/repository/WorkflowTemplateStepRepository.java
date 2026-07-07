package com.infrest.approval_workflow.domain.repository;

import com.infrest.approval_workflow.domain.model.WorkflowTemplateStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowTemplateStepRepository extends JpaRepository<WorkflowTemplateStep, Long> {

    List<WorkflowTemplateStep> findByTemplateIdOrderByStepOrderAsc(Long templateId);

    List<WorkflowTemplateStep> findByApproverEmail(String approverEmail);
}
