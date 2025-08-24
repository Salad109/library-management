INSERT INTO authors (name)
VALUES ('George Orwell'),
       ('J.K. Rowling'),
       ('Antoine de Saint-Exupéry');

INSERT INTO books (isbn, title, publication_year)
VALUES ('9781234567890', '1984', 1949),
       ('9799876543210', 'Harry Potter and the Philosophers Stone', 1997),
       ('123456789X', 'The Little Prince', 1943);

INSERT INTO books_authors (books_isbn, authors_name)
VALUES ('9781234567890', 'George Orwell'),
       ('9799876543210', 'J.K. Rowling'),
       ('123456789X', 'Antoine de Saint-Exupéry');

INSERT INTO copies (book_isbn, status)
VALUES ('9781234567890', 'AVAILABLE'),
       ('9781234567890', 'AVAILABLE'),
       ('9781234567890', 'LOST'),
       ('9799876543210', 'AVAILABLE');

INSERT INTO customer (first_name, last_name, email)
VALUES ('Joe', 'Mama', 'joemama@example.com'),
       ('Jane', 'Mama', null);