ALTER TABLE billing_portal_payment_intents
    ADD COLUMN IF NOT EXISTS plan_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS promo_code_id BIGINT,
    ADD COLUMN IF NOT EXISTS final_amount NUMERIC(12, 2);
