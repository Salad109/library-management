-- Loans have a deadline too now
ALTER TABLE copies
    DROP CONSTRAINT chk_copies_due_at;

UPDATE copies
SET due_at = now() + INTERVAL '28 days'
WHERE status = 'BORROWED';

ALTER TABLE copies
    ADD CONSTRAINT chk_copies_due_at CHECK ((status IN ('RESERVED', 'BORROWED')) = (due_at IS NOT NULL));
