-- Billing portal tables (owned by solaris-billing-api, separate Flyway history)

CREATE TABLE billing_portal_email_challenges (
    id                 BIGSERIAL PRIMARY KEY,
    email_normalized   VARCHAR(255) NOT NULL,
    otp_hash           VARCHAR(255) NOT NULL,
    attempts           INTEGER NOT NULL DEFAULT 0,
    expires_at         TIMESTAMP NOT NULL,
    consumed_at        TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_portal_email_challenges_email
    ON billing_portal_email_challenges (email_normalized, created_at DESC);

CREATE TABLE billing_portal_sessions (
    id          UUID PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id),
    email       VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_portal_sessions_user_id ON billing_portal_sessions (user_id);

CREATE TABLE billing_portal_payment_intents (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          UUID NOT NULL REFERENCES billing_portal_sessions (id),
    organization_id     BIGINT NOT NULL REFERENCES organizations (id),
    product_code        VARCHAR(50) NOT NULL,
    quantity            INTEGER NOT NULL DEFAULT 1,
    provider            VARCHAR(20),
    external_reference  VARCHAR(255),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_portal_payment_intents_session
    ON billing_portal_payment_intents (session_id);
