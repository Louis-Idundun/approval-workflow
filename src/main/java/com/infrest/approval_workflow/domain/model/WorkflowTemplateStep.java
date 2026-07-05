package com.infrest.approval_workflow.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "workflow_template_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTemplateStep extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @NotNull
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @NotBlank
    @Column(name = "approver_email", nullable = false)
    private String approverEmail;

    @Column(name = "approver_role", length = 100)
    private String approverRole;
}
