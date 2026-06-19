# REST API

Base URL: `http://localhost:8080`

Authentication: OAuth 2.0 bearer JWT issued by the Tally Keycloak realm. Swagger UI is public so the contract is discoverable; operations under `/api/**` require a valid token and the appropriate scope.

## Endpoints

| Method | Path | Purpose | Success |
|---|---|---|---|
| `GET` | `/api/expenses` | List owned expenses, newest first | `200` |
| `POST` | `/api/expenses` | Create an owned expense | `201` |
| `GET` | `/api/expenses/{id}` | Read an owned expense | `200` |
| `PUT` | `/api/expenses/{id}` | Replace an owned expense | `200` |
| `DELETE` | `/api/expenses/{id}` | Delete an owned expense | `204` |
| `GET` | `/api/budget` | Read the user's budget | `200` |
| `PUT` | `/api/budget` | Create or replace the user's budget | `200` |

## Required scopes

| Operation | Scope |
|---|---|
| Read expenses | `expenses.read` |
| Create, replace, or delete expenses | `expenses.write` |
| Read budget | `budget.read` |
| Create or replace budget | `budget.write` |

Swagger UI uses Authorization Code with PKCE. API ownership comes from the JWT `sub` claim, not from a request parameter or mutable username.

## Expense request

```json
{
  "amount": 24.75,
  "category": "Food",
  "date": "2026-06-19",
  "description": "Dinner"
}
```

`amount` must be at least `0.01` with at most two decimal places. Category is required and capped at 80 characters; description is capped at 255.

## Expense response

```json
{
  "id": 42,
  "amount": 24.75,
  "category": "Food",
  "date": "2026-06-19",
  "description": "Dinner"
}
```

## Budget request

```json
{
  "amount": 2500.00,
  "period": "monthly"
}
```

`period` accepts only `weekly` or `monthly`.

## Errors

Validation failures return RFC 9457-style problem details with status `400`. Reading or mutating a resource not owned by the authenticated user returns `404`, preventing resource enumeration across accounts. Missing or invalid tokens return `401`; insufficient scopes return `403`.
