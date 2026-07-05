CREATE TABLE departments (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_departments_code ON departments (code);

CREATE TABLE approval_requests (
    id              BIGSERIAL    PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    submitted_by    VARCHAR(255) NOT NULL,
    department_id   BIGINT       NOT NULL REFERENCES departments(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    current_level   INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    version         INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_approval_requests_department_id ON approval_requests (department_id);
CREATE INDEX idx_approval_requests_status ON approval_requests (status);

CREATE TABLE approval_levels (
    id              BIGSERIAL    PRIMARY KEY,
    request_id      BIGINT       NOT NULL REFERENCES approval_requests(id),
    level_number    INT          NOT NULL,
    approver_email  VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    comment         TEXT,
    actioned_at     TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_approval_levels_request_id ON approval_levels (request_id);
CREATE INDEX idx_approval_levels_status ON approval_levels (status);

CREATE TABLE workflow_templates (
    id              BIGSERIAL    PRIMARY KEY,
    department_id   BIGINT       NOT NULL REFERENCES departments(id),
    name            VARCHAR(255) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_templates_department_id ON workflow_templates (department_id);

CREATE TABLE workflow_template_steps (
    id              BIGSERIAL    PRIMARY KEY,
    template_id     BIGINT       NOT NULL REFERENCES workflow_templates(id),
    step_order      INT          NOT NULL,
    approver_email  VARCHAR(255) NOT NULL,
    approver_role   VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_template_steps_template_id ON workflow_template_steps (template_id);
