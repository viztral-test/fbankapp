# fbankapp frontend

Angular 21 client for the fbankapp wallet backend. It has three screens:

| Screen | Route | What it does |
|---|---|---|
| Login | `/login` | Signs in with a username and password |
| Transactions | `/transactions` | Shows the transaction history, newest first, 10 per page |
| Add money | `/add-money` | Simulated wallet top-up |

The current balance is shown at the top of both wallet screens. The wallet screens need a login; opening
one while signed out redirects to `/login`. **Log out** is in the top bar.

All data comes from the backend API described in [../backend/API.md](../backend/API.md). There are no mock
responses, so the backend must be running.

## Prerequisites

- Node.js 20.19 or newer (developed with Node 24 and npm 11)
- The backend running on `http://localhost:8080` (it needs JDK 8)

## Run it

1. Start the backend, from the `backend` folder (in PowerShell, type `.\mvnw8.cmd`):

   ```bat
   mvnw8.cmd spring-boot:run
   ```

2. Start the frontend, from this `frontend` folder:

   ```bash
   npm install
   npm start
   ```

3. Open `http://localhost:4200/` and sign in.

The backend only accepts browser calls from `http://localhost:4200`, so keep the dev server on its default
port. To use another port, also change `fbankapp.cors-allowed-origins` in the backend.

## Demo login (local testing only)

| Username | Password |
|---|---|
| `demo` | `Demo@1234` |

The backend creates this account on startup with an opening balance of 1000.00 USD. Its database is in
memory, so the balance and transactions reset every time the backend restarts.

## Things to know

- **Backend address.** Set in [src/app/core/api.config.ts](src/app/core/api.config.ts).
- **Currency.** Amounts are formatted with the currency code the backend returns for the wallet (USD).
- **Session.** The login token is kept in the browser's `sessionStorage`, so a page reload keeps you signed
  in and closing the tab signs you out. The token expires 60 minutes after login; the next API call then
  returns you to the login screen.
- **Add money rules.** Amount from 0.01 to 10000.00 with at most 2 decimal places; optional note of at most
  80 characters. These match the backend, which checks them again.
- **No automatic retry of top-ups.** The backend adds money on every call it receives. The button is
  disabled while a request is in flight, and if no response arrives the screen asks you to check your
  transactions before trying again.

## Other commands

| Command | Purpose |
|---|---|
| `npm test` | Unit tests (Vitest); add `-- --watch=false` for a single run |
| `npm run build` | Production build into `dist/` |

## Layout

```
src/app/
  core/            API models, auth and wallet services, HTTP interceptor, route guards
  pages/
    login/         Login screen
    wallet-layout/ Top bar, logout and balance shown around the wallet screens
    add-money/     Add Money screen and its amount validator
    transactions/  View Transactions screen
```
