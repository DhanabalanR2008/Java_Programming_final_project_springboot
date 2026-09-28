# ExpenseClaim - Employee Expense Reimbursement Approval Workflow

A complete Spring Boot + JPA + MySQL backend system for managing employee expense reimbursement requests, category-wise policy limit enforcement, multi-stage approval workflows, manager overrides, and finance payments.

---

## 1. Technology Stack

- **Java**: 17
- **Framework**: Spring Boot 3.2.5
- **ORM / Persistence**: Spring Data JPA / Hibernate
- **Validation**: Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Positive`, etc.)
- **Database**: MySQL 8.x (Workbench)
- **API Documentation**: SpringDoc OpenAPI / Swagger UI (v2.3.0)
- **Build Tool**: Apache Maven

---

## 2. Architecture & Package Structure

```text
com.example.expenseclaim
├── ExpenseClaimApplication.java       # Main Spring Boot entry point
├── controller
│   ├── EmployeeController.java        # Employee CRUD REST endpoints
│   ├── ClaimController.java           # Claim submission, approval, override, and payment endpoints
│   └── ExpenseItemController.java     # Claim items and approval history endpoints
├── dto
│   ├── EmployeeRequest.java           # DTO with Bean Validation for creating employees
│   ├── ClaimRequest.java              # DTO with nested item validation for claim submission
│   ├── ExpenseItemRequest.java        # DTO for individual expense items
│   ├── ApprovalRequest.java           # DTO for standard manager approval / rejection
│   ├── OverrideApprovalRequest.java   # DTO requiring mandatory remarks for policy override
│   └── PaymentRequest.java            # DTO with payment reference for finance
├── exception
│   ├── ResourceNotFoundException.java # 404 NOT FOUND for missing entities
│   ├── BusinessRuleException.java     # 400 BAD REQUEST for policy limit & business rule violations
│   ├── InvalidStateException.java     # 409 CONFLICT for illegal lifecycle transitions & duplicates
│   └── GlobalExceptionHandler.java    # @RestControllerAdvice returning uniform JSON error responses
├── model
│   ├── Employee.java                  # JPA Entity: id, name, email, department, managerName
│   ├── Claim.java                     # JPA Entity: id, employee, date, description, total, status, remarks, paymentRef
│   ├── ExpenseItem.java               # JPA Entity: id, claim, category, amount, description, policyLimit, flag
│   └── ApprovalStep.java              # JPA Entity: id, claim, approverName, status, remarks, approvedAt
├── repository
│   ├── EmployeeRepository.java        # Spring Data JPA Repository for Employee
│   ├── ClaimRepository.java           # Spring Data JPA Repository for Claim
│   ├── ExpenseItemRepository.java     # Spring Data JPA Repository for ExpenseItem
│   └── ApprovalStepRepository.java    # Spring Data JPA Repository for ApprovalStep
└── service
    ├── EmployeeService.java           # Business logic for Employee management
    ├── ClaimService.java              # Business logic: submission, total calculation, policy flagging
    ├── ApprovalStepService.java       # Business logic: approvals, overrides, finance payment rules
    └── ExpenseItemService.java        # Read services for expense items
```

---

## 3. Database Model & ER Relationships

Conceptual Tables:
```text
employee (id, name, email [unique], department, manager_name)
    │ 1
    │
    ▼ *
claim (id, employee_id [FK], claim_date, description, total_amount, status, manager_remarks, payment_reference)
    ├── 1 ── * expense_item (id, claim_id [FK], category, amount, description, expense_date, policy_limit, exceeds_policy_limit)
    └── 1 ── * approval_step (id, claim_id [FK], approver_name, status, remarks, approved_at)
```

---

## 4. Category-Wise Policy Limits

| Category | Policy Limit (INR) | Auto-Flagging Behavior |
| :--- | :--- | :--- |
| `TRAVEL` | ₹5,000.00 | If amount > 5000, `exceedsPolicyLimit = true` |
| `FOOD`   | ₹1,000.00 | If amount > 1000, `exceedsPolicyLimit = true` |
| `HOTEL`  | ₹5,000.00 | If amount > 5000, `exceedsPolicyLimit = true` |
| `FUEL`   | ₹3,000.00 | If amount > 3000, `exceedsPolicyLimit = true` |

---

## 5. Workflow State Machine

```text
Employee submits Claim (with 1+ items)
                │
                ▼
        PENDING_APPROVAL
                │
    ┌───────────┴───────────┐
    ▼                       ▼
No Policy Violations    Policy Violation Flagged (exceedsPolicyLimit = true)
    │                       │
    ▼                       ├────────────────────────────┐
Manager Normal Approval     ▼                            ▼
(PUT /approve)          Manager Normal Approval     Manager Override Approval
    │                   (PUT /approve)              (PUT /override-approve)
    │                       │                            │
    │                   REJECTED (400 Bad Request)       │
    ▼                   "Claim requires manager          ▼
 APPROVED                override..."                 APPROVED
    │                                                    │
    └───────────────────────┬────────────────────────────┘
                            │
                            ▼
                    Finance Payment
                    (PUT /pay with paymentReference)
                            │
                            ▼
                          PAID
```

*Note: A claim can also be `REJECTED` by the manager at the `PENDING_APPROVAL` stage via `PUT /api/claims/{id}/reject`. Rejected claims can never be paid.*

---

## 6. REST API Endpoints

### Employee Management
- `POST /api/employees` — Create new employee (validates unique email, non-blank fields)
- `GET /api/employees` — List all employees
- `GET /api/employees/{id}` — Get employee by ID (404 if not found)
- `DELETE /api/employees/{id}` — Delete employee and cascade remove associated claims

### Claim Management
- `POST /api/claims` — Submit an expense claim with itemized list (auto-calculates total, checks policy limits, flags violations, creates approval step)
- `GET /api/claims/{id}` — Get claim details including items and employee
- `GET /api/employees/{employeeId}/claims` — List all claims submitted by an employee
- `GET /api/employees/{employeeId}/claims/pending` — List pending claims for an employee
- `GET /api/claims/{claimId}/items` — List all expense items for a claim
- `GET /api/claims/{claimId}/approval-steps` — View approval history for a claim
- `DELETE /api/claims/{claimId}` — Delete claim by ID (cascades to items and approval steps)
- `DELETE /api/claims` — Delete all claims (batch reset)

### Manager Approvals & Actions
- `PUT /api/claims/{claimId}/approve` — Normal approval (fails with 400 if claim has policy limit violations)
- `PUT /api/claims/{claimId}/reject` — Reject claim with manager remarks
- `PUT /api/claims/{claimId}/override-approve` — Override approval for flagged claims (requires mandatory remarks)

### Finance Payment
- `PUT /api/claims/{claimId}/pay` — Record payment reference and mark claim as `PAID` (enforces manager approval check; rejects unapproved, rejected, or already-paid claims)

---

## 7. Configuration (`application.properties`)

```properties
spring.application.name=expense-claim

# MySQL DataSource
spring.datasource.url=jdbc:mysql://localhost:3306/expense_claim_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=Root

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Swagger / OpenAPI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs

server.port=8080
```

---

## 8. Swagger / OpenAPI Documentation

Once the application is running, access Swagger UI in your browser:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 9. Running and Testing

```bash
# Build the project
mvn clean compile

# Run the project
mvn spring-boot:run
```
