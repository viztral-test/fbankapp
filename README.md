# fbankapp

A small demo web application used to test ViztralAI. It has three screens: Login, Add Money and View Transactions.
Wallet top-ups are simulated; there is no real payment processing.

## Layout

| Folder | Contents |
|---|---|
| `backend/` | Spring Boot REST API (Java 8, Maven, H2 in-memory database) |
| `frontend/` | Angular single-page app |

## Versions

| Component | Version | Notes |
|---|---|---|
| Java | 8 (Temurin 8u504-b01) | Required runtime for the backend |
| Spring Boot | 2.7.18 | Last Spring Boot line that supports Java 8 |
| H2 | 2.1.214 | Managed by Spring Boot 2.7.18 |
| Maven | 3.9.15 | Downloaded by the Maven Wrapper; no global install needed |
| Angular | 21.2.x (LTS) | Needs Node `^20.19`, `^22.12` or `>=24.0` |
| Node.js / npm | 24.15.0 / 11.14.1 | |

## Prerequisites

- **JDK 8**, installed side by side with any other JDK. The backend scripts expect it at
  `%USERPROFILE%\.jdks\jdk8u504-b01`. If yours is elsewhere, set `FBANKAPP_JDK8_HOME` to its folder.
  The system-wide `JAVA_HOME` and default `java` do not need to point at JDK 8.
- **Node.js** 24.x with npm.

Maven and the Angular CLI do not need to be installed globally.

## Run everything

From the project root, `run.cmd` (`.\run.cmd` in PowerShell) starts the backend and the frontend, each in
its own window. The sections below give the same commands one by one.

## Backend

Always build through `mvnw8.cmd`. It sets `JAVA_HOME` to JDK 8 for that one command and then calls the
Maven Wrapper. The build fails on purpose if it is run on any other Java version.

```bat
cd backend
mvnw8.cmd clean verify
mvnw8.cmd spring-boot:run
```

These are Command Prompt (`cmd.exe`) commands. In PowerShell, including the default VS Code terminal, prefix
the script with `.\`, for example `.\mvnw8.cmd spring-boot:run`.

- API base URL: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:fbankdb`, user `sa`, empty password)
- Endpoint guide, configuration and security notes: [backend/API.md](backend/API.md)

The database is in memory, so data resets on every restart.

### Demo account (local testing only)

| Username | Password |
|---|---|
| `demo` | `Demo@1234` |

It is seeded on startup with a 1000.00 USD opening balance. The credentials are public, so do not use them
anywhere but a developer machine; `fbankapp.seed-demo-data=false` turns seeding off.

### API at a glance

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/login` | Issue a bearer token |
| POST | `/api/auth/logout` | Invalidate the token |
| GET | `/api/wallet/balance` | Current balance |
| POST | `/api/wallet/add-money` | Simulated top-up |
| GET | `/api/wallet/transactions` | Transaction history |

Every route except login needs `Authorization: Bearer <token>`.

## Frontend

```bat
cd frontend
npm install
npm start
```

The app is served at `http://localhost:4200`.
