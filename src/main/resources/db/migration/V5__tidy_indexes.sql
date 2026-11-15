-- Same columns as pk_books_authors
DROP INDEX idx_books_authors;
-- Nothing looks books up by an exact author name
DROP INDEX idx_books_authors_reverse;

-- Book search matches author names with ILIKE
CREATE INDEX idx_books_authors_name_pattern ON books_authors USING gin (authors_name gin_trgm_ops);
-- The book list's default order
CREATE INDEX idx_books_title ON books (title, isbn);
-- Registration finds the customer by the activation code's hash
CREATE INDEX idx_customers_activation_code ON customers (activation_code_hash);
