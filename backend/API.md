# fbankapp backend API

Base URL: `http://localhost:8080`. All request and response bodies are JSON.

Wallet top-ups are **simulated**. No payment provider is called and no card or bank details are accepted.

## Demo account (local testing only)

| Username | Password |
|---|---|
| `demo` | `Demo@1234` |

The account is created on startup with an opening balance of 1000.00 USD. These credentials are public, so
they must never be used outside a developer machine. Set `fbankapp.seed-demo-data=false` to skip seeding.

## Authentication

1. `POST /api/auth/login` returns a token.
2. Send it on every other call as `Authorization: Bearer <token>`.
3. The token expires 60 minutes after login (the lifetime is fixed, not extended by activity).
   `POST /api/auth/logout` ends it early.

Every `/api/**` route except login returns `401` without a valid token.

No route takes a user id or wallet id. The server always works out the owner from the token, so a user can
only read and change their own wallet and transactions.

## Routes

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/login` | No | Check credentials and issue a token |
| POST | `/api/auth/logout` | Yes | Invalidate the current token |
| GET | `/api/wallet/balance` | Yes | Current balance of the signed-in user |
| POST | `/api/wallet/add-money` | Yes | Simulated top-up of the signed-in user's wallet |
| GET | `/api/wallet/transactions` | Yes | Signed-in user's transaction history, newest first |

### POST /api/auth/login

Request:

```json
{ "username": "demo", "password": "Demo@1234" }
```

Usernames are not case sensitive. Response `200`:

```json
{
  "token": "vgb4XSkIkREk6svX8IR-z1ctVjhLiiUMVShTHgDODcM",
  "tokenType": "Bearer",
  "expiresAt": "2026-10-10T11:51:11.540Z",
  "username": "demo",
  "displayName": "Demo User"
}
```

Wrong username and wrong password both return `401` with the message `Invalid username or password`.

### POST /api/auth/logout

No body. Response `204`. The token stops working immediately.

### GET /api/wallet/balance

Response `200`:

```json
{ "balance": 1000.00, "currency": "USD", "updatedAt": "2026-10-10T10:51:10.136Z" }
```

### POST /api/wallet/add-money

Request:

```json
{ "amount": 25.50, "note": "Optional description" }
```

| Field | Rules |
|---|---|
| `amount` | Required. From 0.01 to 10000.00, at most 2 decimal places. |
| `note` | Optional, at most 80 characters. Stored as the transaction description. |

Response `201`:

```json
{
  "transaction": {
    "reference": "a0f3fc03-0b2f-4245-ba75-4391b56ae288",
    "type": "TOP_UP",
    "amount": 25.50,
    "balanceAfter": 1025.50,
    "description": "Wallet top-up (simulated)",
    "createdAt": "2026-10-10T10:51:11.607Z"
  },
  "wallet": { "balance": 1025.50, "currency": "USD", "updatedAt": "2026-10-10T10:51:11.607Z" }
}
```

Each call adds money again; there is no idempotency key, so a client should not retry blindly.

### GET /api/wallet/transactions

Query parameters: `page` (default 0) and `size` (default 20, capped at 100). Response `200`:

```json
{
  "items": [
    {
      "reference": "a0f3fc03-0b2f-4245-ba75-4391b56ae288",
      "type": "TOP_UP",
      "amount": 25.50,
      "balanceAfter": 1025.50,
      "description": "Wallet top-up (simulated)",
      "createdAt": "2026-10-10T10:51:11.607Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 2,
  "totalPages": 1
}
```

`type` is `TOP_UP` or `OPENING_BALANCE`.

## Money and dates

- Money is held as `BigDecimal` and stored as `DECIMAL(19,2)`. In JSON it is a plain number with two
  decimals. A quoted string such as `"25.50"` is also accepted for `amount`.
- Timestamps are ISO-8601 in UTC.

## Errors

```json
{
  "timestamp": "2026-10-10T10:51:11.666Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/wallet/add-money",
  "fieldErrors": { "amount": "amount must be at least 0.01" }
}
```

`fieldErrors` appears only on validation failures.

| Status | When |
|---|---|
| 400 | Validation failure, malformed JSON, or a bad query parameter |
| 401 | Wrong credentials, or a missing, invalid or expired token |
| 404 | Unknown route |
| 500 | Unexpected error (details are in the server log only) |

## Try it

Windows Command Prompt (`cmd.exe`):

```bat
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"demo\",\"password\":\"Demo@1234\"}"
set TOKEN=<token from the response>
curl http://localhost:8080/api/wallet/balance -H "Authorization: Bearer %TOKEN%"
curl -X POST http://localhost:8080/api/wallet/add-money -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"amount\":25.50}"
curl http://localhost:8080/api/wallet/transactions -H "Authorization: Bearer %TOKEN%"
```

## Configuration

Set in `src/main/resources/application.properties`, or override on the command line
(for example `mvnw8.cmd spring-boot:run -Dspring-boot.run.arguments=--fbankapp.token-ttl=15m`).

| Property | Default | Meaning |
|---|---|---|
| `fbankapp.token-ttl` | `60m` | Token lifetime |
| `fbankapp.seed-demo-data` | `true` | Create the demo account on startup |
| `fbankapp.currency` | `USD` | Currency code given to new wallets |
| `fbankapp.cors-allowed-origins` | `http://localhost:4200` | Browser origins allowed to call `/api/**` (comma-separated) |

## Security notes

This is a local demo backend. What it does:

- Passwords are stored as BCrypt hashes.
- Tokens are 256-bit random values. Only their SHA-256 hash is stored, so the database does not contain
  usable tokens.
- Tokens travel in the `Authorization` header, not in cookies, so cross-site request forgery does not apply.
  CORS allows only the configured origins.
- A top-up locks the wallet row, so simultaneous top-ups cannot lose an update.

What it deliberately leaves out, and would need before any real use:

- HTTPS. The server speaks plain HTTP, which is only acceptable on localhost.
- Login rate limiting or account lockout.
- Protection for the H2 console. `/h2-console` is open with user `sa` and an empty password.
- User registration, password change and token refresh.
