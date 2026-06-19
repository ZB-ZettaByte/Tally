# Architecture

## Component model

```mermaid
flowchart TB
    subgraph Adapters
        Swing[LoginFrame / MainApp / Panels]
        Controllers[REST Controllers + DTOs]
    end

    subgraph Application
        UserService
        ExpenseService
        BudgetService
        Analytics[Pure Analytics Engine]
    end

    subgraph Infrastructure
        Security[Spring Security JWT Resource Server]
        Keycloak[Keycloak OIDC]
        Repositories[Spring Data JPA]
        ORM[Hibernate ORM]
        DB[(H2)]
        OpenAPI[springdoc OpenAPI]
    end

    Swing --> UserService
    Swing --> ExpenseService
    Swing --> BudgetService
    Swing --> Analytics
    Controllers --> Security
    Keycloak --> Security
    Security --> UserService
    Controllers --> ExpenseService
    Controllers --> BudgetService
    Controllers --> OpenAPI
    UserService --> Repositories
    ExpenseService --> Repositories
    BudgetService --> Repositories
    Repositories --> ORM --> DB
```

## Data model

```mermaid
erDiagram
    USER ||--o{ EXPENSE : owns
    USER ||--o| BUDGET : configures

    USER {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar external_subject UK "nullable for Swing-only users"
        varchar role
    }
    EXPENSE {
        bigint id PK
        bigint user_id FK
        decimal amount "DECIMAL(19,2)"
        varchar category
        date date
        varchar description
    }
    BUDGET {
        bigint id PK
        bigint user_id FK,UK
        decimal amount "DECIMAL(19,2)"
        varchar period
    }
```

The budget's unique `user_id` foreign key encodes the one-budget-per-user invariant in both the object model and database while allowing the v1 singleton row to be migrated without data loss.

## Request path

```mermaid
sequenceDiagram
    participant Client
    participant Security as Spring Security
    participant Controller
    participant Service
    participant Hibernate
    participant DB

    Client->>Security: Bearer JWT + request
    Security->>Security: verify signature, issuer, expiry, scopes
    Security->>Controller: authenticated Jwt
    Controller->>Service: owner + validated command
    Service->>Hibernate: user-scoped repository call
    Hibernate->>DB: parameterized SQL in transaction
    DB-->>Client: owned resource response
```

## Boundaries and invariants

- Controllers never accept an owner ID; they derive the owner from the immutable OIDC `sub` claim.
- Spring Security maps token scopes to `SCOPE_...` authorities before controller dispatch.
- Swing passes the authenticated `User` into the same services.
- Services define transaction boundaries and require an owner for every expense or budget operation.
- DTOs prevent persistence entities and password fields from leaking over HTTP.
- Monetary persistence uses `BigDecimal`; floating point is limited to visualization and statistical algorithms.
- Repository method names include `Owner`, making accidental global queries visible during review.

## Production evolution

For a hosted version, add Flyway, PostgreSQL, OAuth2/OIDC, Testcontainers, structured audit events, and optimistic locking. These changes fit behind the current adapter and repository boundaries without replacing the Swing client or domain services.
