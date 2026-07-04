# solaris-billing-api

External billing portal for Solaris ERP. Sprint 1 provides email OTP verification, billing sessions, and a static portal to list billable organizations.

## Stack

- Java 21, Spring Boot 3.5
- PostgreSQL (shared Neon DB with `solaris-api`)
- Flyway migrations in `flyway_billing_history`
- Resend for OTP emails
- Static portal served from `/`

## Local development

1. Copy env template:

```bash
cp .env.example .env
```

2. Fill `.env` with the same Neon credentials as `solaris-api` (`DATABASE_URL` must start with `jdbc:postgresql://`).

3. Start the server:

```powershell
.\run-local.ps1
```

Or manually:

```powershell
$env:DATABASE_URL="jdbc:postgresql://..."
$env:DATABASE_USERNAME="..."
$env:DATABASE_PASSWORD="..."
mvn spring-boot:run
```

Open http://localhost:8081

### Troubleshooting

| Symptom | Cause |
|---------|--------|
| `ERR_CONNECTION_REFUSED` on :8081 | The server is not running (nothing listening on that port). |
| `'url' must start with "jdbc"` | `DATABASE_URL` missing or invalid. |
| `DATABASE_URL is not configured` | No `.env` / env vars before startup. |

## Public API (Sprint 1)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/public/verify-email` | Send OTP to a registered Solaris email with billing access |
| POST | `/api/v1/public/confirm-email` | Verify OTP and create billing session |
| GET | `/api/v1/public/organizations?sessionId=` | List organizations for the session |

## Render deploy

1. Create a Web Service from this repo.
2. Set env vars: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `RESEND_API_KEY`, `EMAIL_FROM`, `BILLING_PORTAL_URL`.
3. Use Docker or `mvn -DskipTests package` + `java -jar target/solaris-billing-api-0.0.1-SNAPSHOT.jar`.
4. Point `pay.solarismanager.com` to this service.

## Roadmap

- **Sprint 2:** Mercado Pago checkout + webhooks
- **Sprint 3:** Stripe checkout + activation emails
- **Sprint 4:** Link from main app Billing page
