# Database Schema Specification

The schema is defined for MySQL 8.0+ and fully compatible with MySQL dialect in embedded H2 database for local testing.

## Entity Relationship Overview

```mermaid
erDiagram
    USERS ||--o{ FAMILY_LINKS : "participates"
    USERS ||--o{ TRANSACTIONS : "owns"
    USERS ||--o{ BUDGETS : "defines"
    USERS ||--o{ SAVINGS_GOALS : "tracks"
    USERS ||--o{ INVESTMENTS : "holds"
    USERS ||--o{ SIP_RECORDS : "schedules"
    USERS ||--o{ STATEMENT_IMPORTS : "uploads"
    USERS ||--o{ AUDIT_EVENTS : "triggers"

    USERS {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR display_name
        VARCHAR role
        VARCHAR invitation_code UK
        VARCHAR status
        TIMESTAMP created_at
    }

    FAMILY_LINKS {
        BIGINT id PK
        BIGINT parent_user_id FK
        BIGINT student_user_id FK
        VARCHAR status
        VARCHAR requested_by
        TIMESTAMP approved_at
        TIMESTAMP created_at
    }

    TRANSACTIONS {
        BIGINT id PK
        BIGINT owner_student_id FK
        VARCHAR type
        DECIMAL amount
        VARCHAR category
        VARCHAR description
        DATE transaction_date
        VARCHAR payment_method
        VARCHAR source
        VARCHAR reference_hash
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    BUDGETS {
        BIGINT id PK
        BIGINT student_user_id FK
        VARCHAR category
        VARCHAR period_month
        DECIMAL limit_amount
        DECIMAL warning_threshold
        TIMESTAMP created_at
    }

    SAVINGS_GOALS {
        BIGINT id PK
        BIGINT student_user_id FK
        VARCHAR name
        DECIMAL target_amount
        DECIMAL saved_amount
        DATE target_date
        VARCHAR status
        TIMESTAMP created_at
    }

    INVESTMENTS {
        BIGINT id PK
        BIGINT student_user_id FK
        VARCHAR asset_type
        VARCHAR asset_name
        DECIMAL invested_amount
        DECIMAL current_value
        TIMESTAMP last_updated
        TIMESTAMP created_at
    }

    SIP_RECORDS {
        BIGINT id PK
        BIGINT student_user_id FK
        VARCHAR fund_name
        DECIMAL monthly_amount
        DATE start_date
        INT due_day_of_month
        VARCHAR payment_status
        DECIMAL assumed_return_rate
        INT duration_months
        TIMESTAMP created_at
    }

    STATEMENT_IMPORTS {
        BIGINT id PK
        BIGINT owner_user_id FK
        VARCHAR filename
        INT total_rows
        INT imported_rows
        INT duplicate_rows
        VARCHAR status
        TIMESTAMP created_at
    }

    AUDIT_EVENTS {
        BIGINT id PK
        BIGINT actor_user_id FK
        VARCHAR action
        VARCHAR target_type
        VARCHAR target_id
        VARCHAR metadata
        TIMESTAMP timestamp
    }
```

## Relational Constraints & Types
- **Money Values**: Always `DECIMAL(12,2)` to guarantee exact accounting calculations without floating-point inaccuracies.
- **Indexes**: Indexed on `owner_student_id`, `transaction_date`, `category`, and `family_links (parent_user_id, student_user_id, status)`.
- **Foreign Keys**: `ON DELETE CASCADE` on student resources so data remains referentially intact.
