# MediSync frontend

The React application uses React Router for navigation and Axios for API calls.

## Run locally

Install dependencies and start the Vite server:

```powershell
Set-Location pharmacy-frontend
npm ci
npm run dev
```

`pharmacy-frontend/.env` sets `VITE_API_URL` to `http://localhost:8090`, the
backend's configured port.

## Backend configuration

The backend requires a fresh, private JWT signing key supplied through
`JWT_SECRET`; do not put the key in source control. In PowerShell, generate a
32-byte key for the backend process:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Password reset messages require SMTP configuration through `SMTP_HOST`,
`SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, and `SMTP_FROM`. Set
`FRONTEND_URL` to the public frontend origin when deploying so reset links point
to the correct site.

The database-backed Spring context test is opt-in with
`RUN_DB_INTEGRATION_TESTS=true`; it requires a configured MySQL database and
JWT key.

The bootstrap administrator is enabled by default. Configure it with
`ADMIN_USERNAME`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD` before starting the
backend. The defaults are `admin`, `admin@medisync.example`, and
`Admin@12345`; override the password for any non-local deployment. The seeder
creates the account only if that username does not already exist and never
logs the password.

The sample South African pharmacy medicine catalogue and its stock batches are
seeded by default. Set `app.seed.sample-medications=false` in the backend
environment to disable this data seed; the flag defaults to `true` when unset.
