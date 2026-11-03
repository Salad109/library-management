-- Everyone logs in with an email kept on users.
ALTER TABLE users
    ADD COLUMN email VARCHAR(150);

-- Customers keep their customer email. Anyone without one gets one made from the username.
UPDATE users
SET email = lower(coalesce(
        (SELECT c.email FROM customers c WHERE c.id = users.customer_id),
        username || '@example.com'));

ALTER TABLE users
    ALTER COLUMN email SET NOT NULL,
    ADD CONSTRAINT uk_users_email UNIQUE (email),
    DROP COLUMN username;

ALTER TABLE customers
    DROP COLUMN email;
