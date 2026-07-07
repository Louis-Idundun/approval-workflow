# Approval Workflow Service

Multi-level approval workflow engine for departmental requests. Built with Spring Boot 3, Java 17, PostgreSQL, and Flyway.

Requests move through a configurable sequence of approvers defined by workflow templates. A request is only fully approved when all required levels have been approved in strict order.

---

## Quick Start

### Prerequisites

- **Java 17+** (tested with Corretto 17)
- **Maven 3.9+** (bundled `./mvnw` autodetects your JDK)
- **Docker** (optional — for the Compose workflow)
- **PostgreSQL 16** (only if running locally without Docker)

### Run with Docker Compose (recommended)

```bash
docker compose up --build
```

This starts PostgreSQL 16 and the application on `http://localhost:8080`. Flyway runs the migrations automatically. Seed data (Engineering department + 3-level template) is loaded by `V2__seed_data.sql` — you can submit a request immediately.

### Run locally

```bash
# Start PostgreSQL, then:
export DB_URL=jdbc:postgresql://localhost:5432/approval_workflow
export DB_USERNAME=app_user
export DB_PASSWORD=app_secret
./mvnw spring-boot:run
```

### Run tests

```bash
./mvnw clean test
```

Tests use an H2 in-memory database and require no external infrastructure.

### View API docs

Start the application and open `http://localhost:8080/swagger-ui.html`.

---

## API Overview

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/departments` | Create a department |
| `GET` | `/api/v1/departments` | List all departments |
| `GET` | `/api/v1/departments/{id}` | Get department by ID |
| `POST` | `/api/v1/departments/{deptId}/templates` | Create a workflow template with steps |
| `GET` | `/api/v1/departments/{deptId}/templates` | List templates for a department |
| `PUT` | `/api/v1/departments/{deptId}/templates/{tplId}/activate` | Activate a template (deactivates others) |
| `POST` | `/api/v1/requests` | Submit an approval request |
| `POST` | `/api/v1/requests/{id}/action` | Approve or reject the current level |
| `GET` | `/api/v1/requests/{id}` | Get request with all approval levels |
| `GET` | `/api/v1/requests?departmentId=` | List requests by department (optional `&status=`) |
| `GET` | `/api/v1/requests?approverEmail=` | List requests pending an approver's action |
| `DELETE` | `/api/v1/requests/{id}/cancel?cancelledBy=` | Cancel a PENDING or IN_PROGRESS request |

### Curl examples

```bash
# 1. Create a department
curl -s -X POST http://localhost:8080/api/v1/departments \
  -H 'Content-Type: application/json' \
  -d '{"name":"Engineering","code":"ENG"}' | jq

# 2. Create a workflow template with 2 approval steps
curl -s -X POST http://localhost:8080/api/v1/departments/1/templates \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Engineering Review",
    "steps": [
      {"stepOrder": 1, "approverEmail": "lead@example.com", "approverRole": "Tech Lead"},
      {"stepOrder": 2, "approverEmail": "manager@example.com", "approverRole": "Manager"}
    ]
  }' | jq

# 3. Activate the template
curl -s -X PUT http://localhost:8080/api/v1/departments/1/templates/1/activate | jq

# 4. Submit an approval request
curl -s -X POST http://localhost:8080/api/v1/requests \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "New Feature",
    "description": "Dashboard v2",
    "submittedBy": "dev@example.com",
    "departmentId": 1
  }' | jq

# 5. Approve level 1
curl -s -X POST http://localhost:8080/api/v1/requests/1/action \
  -H 'Content-Type: application/json' \
  -d '{
    "approverEmail": "lead@example.com",
    "action": "APPROVE",
    "comment": "Looks good"
  }' | jq

# 6. Approve level 2 (triggers full approval)
curl -s -X POST http://localhost:8080/api/v1/requests/1/action \
  -H 'Content-Type: application/json' \
  -d '{
    "approverEmail": "manager@example.com",
    "action": "APPROVE",
    "comment": "Approved"
  }' | jq

# 7. Get final status
curl -s http://localhost:8080/api/v1/requests/1 | jq

# 8. Reject (on a different request)
curl -s -X POST http://localhost:8080/api/v1/requests/2/action \
  -H 'Content-Type: application/json' \
  -d '{
    "approverEmail": "lead@example.com",
    "action": "REJECT",
    "comment": "Not ready"
  }' | jq
```

---

## Assumptions

- A **department must have an active `WorkflowTemplate`** before any request can be submitted. If none exists, `submitRequest` throws `IllegalStateException`.
- Approval levels must be actioned in **strict sequential order** (level 1 → level 2 → …). Skipping a level is rejected.
- Once a request reaches `APPROVED` or `REJECTED`, **no further actions are permitted**. The state machine is terminal.
- `approverEmail` is the sole identifier for approvers. There is **no authentication layer** — this MVP trusts the caller to supply the correct email.
- Cancellation is only permitted while the request is `PENDING` or `IN_PROGRESS`.
- The `cancelledBy` parameter is accepted but **not enforced** at the service layer in the current implementation (the spec only checks status, not submitter identity).

---

## Design Decisions

### Optimistic locking with @Version
`ApprovalRequest` carries a `@Version` field. If two transactions read and attempt to update the same request concurrently (e.g., two approvers acting at once), the second transaction fails with `OptimisticLockingFailureException`. The `GlobalExceptionHandler` surfaces this as **HTTP 409 Conflict** with a descriptive message, allowing the client to retry. This avoids pessimistic database locks while keeping approval transitions atomic.

### Flyway for schema management
All schema changes are Flyway migrations checked into version control. The application uses `ddl-auto: validate` so that Hibernate validates entities against the migration scripts at startup — catching mismatches early without allowing Hibernate to auto-create tables.

### Java records for DTOs
Request/response DTOs are Java records — immutable by default, concise, and free of boilerplate. Bean Validation constraints (`@NotBlank`, `@NotNull`, `@Size`) are declared compactly on the record components.

### Centralised error handling
A single `@RestControllerAdvice` (`GlobalExceptionHandler`) catches every exception type and returns a consistent JSON envelope:
```json
{
  "timestamp": "2026-07-07T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "title: must not be blank",
  "path": "/api/v1/requests"
}
```

### Template-request separation
`WorkflowTemplate` / `WorkflowTemplateStep` define the *intended* approval chain, while `ApprovalRequest` / `ApprovalLevel` capture the *actual* approval state. This allows templates to evolve (new steps, different approvers) without affecting in-flight requests — each request snapshots its own levels at submission time.

### Status state machine
The request lifecycle is a strict state machine enforced at the service layer:

```
PENDING ──► IN_PROGRESS ──► APPROVED
                │
                └──► REJECTED
                │
                └──► CANCELLED (only from PENDING or IN_PROGRESS)
```

Transitions are validated in `processApprovalAction` and `cancelRequest` before any writes occur.

---

## Project Structure

```
src/main/java/com/infrest/approval_workflow/
├── ApprovalWorkflowApplication.java     # Spring Boot entry point
├── api/
│   ├── config/
│   │   ├── JpaAuditingConfig.java       # @EnableJpaAuditing
│   │   └── OpenApiConfig.java           # Swagger/OpenAPI metadata
│   ├── controller/
│   │   ├── DepartmentController.java
│   │   ├── WorkflowTemplateController.java
│   │   └── ApprovalWorkflowController.java
│   ├── dto/
│   │   ├── ApprovalActionDto.java
│   │   ├── ApprovalLevelResponse.java
│   │   ├── ApprovalRequestResponse.java
│   │   ├── CreateApprovalRequestDto.java
│   │   ├── CreateWorkflowTemplateRequest.java
│   │   ├── CreateWorkflowTemplateStepRequest.java
│   │   ├── DepartmentDto.java
│   │   ├── WorkflowTemplateDto.java
│   │   └── WorkflowTemplateStepDto.java
│   └── exception/
│       ├── ApiErrorResponse.java
│       ├── EntityNotFoundException.java
│       └── GlobalExceptionHandler.java
├── domain/
│   ├── enums/
│   │   ├── ApprovalLevelStatus.java
│   │   └── RequestStatus.java
│   ├── model/
│   │   ├── BaseEntity.java              # @MappedSuperclass with created/updated audit
│   │   ├── Department.java
│   │   ├── WorkflowTemplate.java
│   │   ├── WorkflowTemplateStep.java
│   │   ├── ApprovalRequest.java         # @Version for optimistic locking
│   │   └── ApprovalLevel.java
│   └── repository/
│       ├── DepartmentRepository.java
│       ├── WorkflowTemplateRepository.java
│       ├── WorkflowTemplateStepRepository.java
│       ├── ApprovalRequestRepository.java
│       └── ApprovalLevelRepository.java
└── service/
    ├── ApprovalWorkflowService.java
    ├── DepartmentService.java
    ├── WorkflowTemplateService.java
    └── impl/
        ├── ApprovalWorkflowServiceImpl.java
        ├── DepartmentServiceImpl.java
        └── WorkflowTemplateServiceImpl.java

src/main/resources/
├── application.yaml                     # PostgreSQL config with env var placeholders
└── db/migration/
    ├── V1__init_schema.sql              # All 5 tables with FK, indexes, unique constraints
    └── V2__seed_data.sql                # Engineering department + 3-level template

src/test/
├── java/...
│   ├── ApprovalWorkflowApplicationTests.java
│   ├── service/impl/ApprovalWorkflowServiceImplTest.java   # 9 unit tests
│   └── api/controller/ApprovalWorkflowControllerTest.java  # 4 integration tests
└── resources/application.yml            # H2 in-memory config for tests
```

---

## Error Codes

| HTTP | Exception | When |
|------|-----------|------|
| 400 | `IllegalStateException` | Invalid workflow transition (wrong status, wrong approver, no template) |
| 400 | `IllegalArgumentException` | Duplicate department code |
| 400 | `MethodArgumentNotValidException` | Bean validation failure on request body |
| 404 | `EntityNotFoundException` | Department, request, or template not found |
| 409 | `OptimisticLockingFailureException` | Concurrent modification conflict (retry) |
| 500 | `Exception` (generic) | Unexpected server error |
