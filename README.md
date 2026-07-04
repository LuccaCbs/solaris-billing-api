# solaris-billing-api

External billing portal for Solaris ERP. Sprint 1 provides email OTP verification, billing sessions, and a static portal to list billable organizations.

## Stack

- Java 21, Spring Boot 3.5
- PostgreSQL (shared Neon DB with `solaris-api`)
- Flyway migrations in `flyway_billing_history`
- Resend for OTP emails
- Static portal served from `/`

## Local development

```bash
export DATABASE_URL=jdbc:postgresql://...
export DATABASE_USERNAME=...
export DATABASE_PASSWORD=...
export RESEND_API_KEY=re_...   # optional; OTP prints to console if omitted

mvn spring-boot:run
```

Open http://localhost:8081

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
