CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
CREATE INDEX IF NOT EXISTS idx_authors_name ON authors(name);
CREATE INDEX IF NOT EXISTS idx_books_authors_books_isbn ON books_authors(books_isbn);
CREATE INDEX IF NOT EXISTS idx_books_authors_authors_name ON books_authors(authors_name);
CREATE INDEX IF NOT EXISTS idx_copies_book_isbn ON copies(book_isbn);
CREATE INDEX IF NOT EXISTS idx_copies_status ON copies(status);
CREATE INDEX IF NOT EXISTS idx_copies_customer_id ON copies(customer_id);