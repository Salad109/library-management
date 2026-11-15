-- The customer points at its account
ALTER TABLE customers
    ADD COLUMN user_id BIGINT;

UPDATE customers c
SET user_id = u.id
FROM users u
WHERE u.customer_id = c.id;

ALTER TABLE customers
    ADD CONSTRAINT uk_customers_user UNIQUE (user_id),
    ADD CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE users
    DROP COLUMN customer_id;
