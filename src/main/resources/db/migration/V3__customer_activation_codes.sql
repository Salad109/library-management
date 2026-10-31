ALTER TABLE customers
    ADD COLUMN activation_code_hash       VARCHAR(64),
    ADD COLUMN activation_code_expires_at TIMESTAMPTZ;
