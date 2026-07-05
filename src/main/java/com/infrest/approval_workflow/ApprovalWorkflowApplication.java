package com.infrest.approval_workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ApprovalWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApprovalWorkflowApplication.class, args);
    }
}
