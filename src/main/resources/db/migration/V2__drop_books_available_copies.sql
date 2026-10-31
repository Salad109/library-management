-- Book.availableCopies is computed from copies (@Formula)
ALTER TABLE books
    DROP CONSTRAINT chk_books_copies;
ALTER TABLE books
    DROP COLUMN available_copies;
