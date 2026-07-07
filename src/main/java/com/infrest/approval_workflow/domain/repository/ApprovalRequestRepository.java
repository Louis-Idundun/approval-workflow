package com.infrest.approval_workflow.domain.repository;

import com.infrest.approval_workflow.domain.model.ApprovalRequest;
import com.infrest.approval_workflow.domain.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByDepartmentIdAndStatus(Long departmentId, RequestStatus status);

    List<ApprovalRequest> findByDepartmentId(Long departmentId);

    List<ApprovalRequest> findBySubmittedBy(String submittedBy);
}
