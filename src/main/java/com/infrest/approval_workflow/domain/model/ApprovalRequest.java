package com.infrest.approval_workflow.domain.model;

import com.infrest.approval_workflow.domain.model.enums.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "approval_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Column(name = "submitted_by", nullable = false)
    private String submittedBy;

    @NotNull
    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "current_level", nullable = false)
    @Builder.Default
    private Integer currentLevel = 0;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;
}
