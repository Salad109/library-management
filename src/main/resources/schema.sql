CREATE
EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_books_title_pattern ON books USING gin(title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_books_authors ON books_authors(books_isbn, authors_name);
CREATE INDEX IF NOT EXISTS idx_books_authors_reverse ON books_authors(authors_name, books_isbn);
CREATE INDEX IF NOT EXISTS idx_copies_book_status ON copies(book_isbn, status);
CREATE INDEX IF NOT EXISTS idx_copies_customer_id ON copies(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_name ON customers(last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_customer_lastname_pattern ON customers USING gin(last_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_customer_email ON customers(email) WHERE email IS NOT NULL;