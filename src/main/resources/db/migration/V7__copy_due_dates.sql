ALTER TABLE copies
    ADD COLUMN due_at   TIMESTAMPTZ,
    ADD COLUMN renewals INTEGER NOT NULL DEFAULT 0;

UPDATE copies
SET due_at = now() + INTERVAL '3 days'
WHERE status = 'RESERVED';

ALTER TABLE copies
    ADD CONSTRAINT chk_copies_due_at CHECK ((status = 'RESERVED') = (due_at IS NOT NULL));
