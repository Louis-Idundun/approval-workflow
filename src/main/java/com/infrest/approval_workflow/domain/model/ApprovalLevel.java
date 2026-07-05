package com.infrest.approval_workflow.domain.model;

import com.infrest.approval_workflow.domain.model.enums.ApprovalLevelStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_levels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalLevel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @NotNull
    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;

    @NotBlank
    @Column(name = "approver_email", nullable = false)
    private String approverEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalLevelStatus status = ApprovalLevelStatus.PENDING;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;
}
