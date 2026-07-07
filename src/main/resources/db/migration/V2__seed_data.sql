INSERT INTO departments (name, code)
VALUES ('Engineering', 'ENG');

INSERT INTO workflow_templates (department_id, name, active)
VALUES (1, 'Engineering Approval', TRUE);

INSERT INTO workflow_template_steps (template_id, step_order, approver_email, approver_role)
VALUES (1, 1, 'tech-lead@example.com', 'Tech Lead'),
       (1, 2, 'eng-manager@example.com', 'Engineering Manager'),
       (1, 3, 'cto@example.com', 'CTO');
