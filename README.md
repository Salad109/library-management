# Library Management System

A Spring Boot REST API for managing books and their copies in a library.

## Features

- Book management with ISBN validation and author relationships
- Author management with many-to-many relationships to books
- Copy tracking with status management (available, borrowed, reserved, lost)
- Search functionality across books, copies and authors
- Robust integration and unit tests with >80% coverage

## Tech Stack

- Java 21 + Spring Boot 3.5.0
- Spring Data JPA with H2 (planning PostgreSQL migration)
- Undertow web server (replaced default Tomcat for better performance)
- JUnit 5 + MockMvc for testing
- Maven

## Startup Guide

```bash
./mvnw spring-boot:run
```

The API runs on `http://localhost:8080`. H2 console is available at `/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`,
username: `sa`, no password)

## API Endpoints

### Response Codes

- `200 OK` - Successful GET, PUT
- `201 Created` - Successful POST
- `204 No Content` - Successful DELETE
- `400 Bad Request` - Invalid request data
- `404 Not Found` - Resource doesn't exist
- `409 Conflict` - Duplicate resource (e.g., ISBN already exists)

### Books

- `GET /api/books` - List all books (paginated)
- `GET /api/books/{isbn}` - Get book by ISBN
- `GET /api/books/search` - Search books by title, author, year, ISBN (paginated)
    - Example: `/api/books/search?title=...&authorName=...&page=...&size=...`
- `POST /api/books` - Create book
- `PUT /api/books/{isbn}` - Update book
- `DELETE /api/books/{isbn}` - Delete book

### Copies

- `GET /api/copies` - List all copies (paginated)
- `GET /api/copies/{id}` - Get copy by ID
- `GET /api/copies/book/{isbn}` - Get copies of a specific book (paginated)
- `GET /api/copies/book/{isbn}/available` - Get available copies (paginated)
- `GET /api/copies/book/{isbn}/count` - Count available copies
- `POST /api/copies` - Create copy
- `PUT /api/copies/{copyId}/borrow/{customerId}` - Borrow copy
- `PUT /api/copies/{copyId}/return/{customerId}` - Return copy
- `PUT /api/copies/{copyId}/reserve/{customerId}` - Reserve copy
- `PUT /api/copies/{copyId}/undo-reserve/{customerId}` - Cancel reservation
- `PUT /api/copies/{copyId}/lost/{customerId}` - Mark copy as lost
- `DELETE /api/copies/{id}` - Delete copy

#### Copy Status Values

- `AVAILABLE` - Can be borrowed or reserved
- `BORROWED` - Currently checked out
- `RESERVED` - On hold for a customer
- `LOST` - Missing from inventory

### Authors

- `GET /api/authors` - List all authors (paginated)
- `GET /api/authors/{name}` - Get author by name

### Customers

- `GET /api/customers` - List all customers (paginated)
- `GET /api/customers/{id}` - Get customer by ID
- `POST /api/customers` - Create customer
- `PUT /api/customers/{id}` - Update customer
- `DELETE /api/customers/{id}` - Delete customer

## Example Requests

### Create a Book

```json
POST /api/books
{
  "title": "1984",
  "publicationYear": 1984,
  "isbn": "9789876543210",
  "authors": [
    {
      "name": "George Orwell"
    }
  ]
}
```

Authors will be matched and created automatically if they don't exist yet.

### Create a Copy

```json
POST /api/copies
{
  "book": {
    "isbn": "9789876543210"
  },
  "status": "AVAILABLE"
}
```

### Create a Customer

```json
POST /api/customers
{
  "firstName": "Joe",
  "lastName": "Mama", 
  "email": "joe.mama@example.com"
}
```

### Borrow a Copy

```json
PUT /api/copies/1/borrow/1
```

This will change the copy status to `BORROWED` and associate it with the customer.

## Progress

- [x] Basic book CRUD operations
- [x] ISBN regex validation and duplicate prevention
- [x] Many-to-many author-book relationships with author duplicate prevention
- [x] Book search by title, author, year, ISBN
- [x] Copy status tracking (available, borrowed, reserved, lost)
- [x] State transition validation for copies
- [x] Comprehensive test coverage written while procrastinating adding new features (ongoing)
- [x] Customer management
- [x] Borrowing and renting system with customers
- [ ] Book categories/genres
- [ ] Logging and monitoring
- [ ] Proper API documentation
- [ ] Authentication
- [ ] PostgreSQL migration from H2
- [ ] Docker
- [ ] Microcontroller-based book scanner