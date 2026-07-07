package com.infrest.approval_workflow.domain.repository;

import com.infrest.approval_workflow.domain.model.ApprovalLevel;
import com.infrest.approval_workflow.domain.enums.ApprovalLevelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalLevelRepository extends JpaRepository<ApprovalLevel, Long> {

    List<ApprovalLevel> findByRequestIdOrderByLevelNumberAsc(Long requestId);

    List<ApprovalLevel> findByApproverEmailAndStatus(String approverEmail, ApprovalLevelStatus status);
}
