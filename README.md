# Library Management System

A library management system built with Spring Boot, featuring public book catalog
browsing, customer self-service reservations, librarian workflow management,
role-based authentication, and real-time operational monitoring.

## Features

- Book management with ISBN validation and author relationships
- Author management with many-to-many relationships to books
- Copy tracking with status management (available, borrowed, reserved, lost)
- Customer management with email validation
- Borrowing and reservation system
- Authentication and authorization with Spring Security
- Password hashing and secure user management
- Search functionality across books, copies and authors
- Application monitoring with Prometheus and Grafana
- 94% test coverage with comprehensive integration and unit testing

## Tech Stack

- Java 21 + Spring Boot 3.5.0
- Spring Data JPA with PostgreSQL
- Spring Security for authentication and role-based authorization
- Slf4j for logging
- Prometheus for metrics collection
- Grafana for monitoring dashboards
- Docker Compose for containerization
- Swagger for API documentation
- Undertow web server
- JUnit 5 + MockMvc for testing
- Maven

## Starting Guide

```bash
# Clone environment
cp .env.example .env
# Edit .env with your database credentials. For example:
# POSTGRES_USER=library_admin
# POSTGRES_PASSWORD=secret123
# POSTGRES_DB=library_db

# Start all services
docker-compose up
```

The application runs on `http://localhost:8080`. API documentation is available
at http://localhost:8080/swagger-ui.html.
Monitoring dashboard is available
via Grafana and Prometheus.

## Monitoring Setup Guide

1. Visit Grafana at `http://localhost:3000` (admin/admin)
2. Create a new data source:
    - Type: Prometheus
    - URL: `http://prometheus:9090`
    - Use default settings for everything else
3. Click "Save & Test"
4. Import dashboard from `grafana/dashboard.json`
5. Select the created Prometheus data source
6. Generate sample data by running `exampleRequests/complete-workflow.http`

<img src="https://github.com/user-attachments/assets/41e175c4-f124-4dc9-a132-f77a5e108fe1"
alt="dashboard screenshot" width="100%"/>
Dashboard screenshot during ~100 RPS load simulation using the config in `grafana/dashboard.json`.

## API Endpoints

### Response Codes

- `200 OK` - Successful GET, PUT
- `201 Created` - Successful POST
- `204 No Content` - Successful DELETE
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource doesn't exist
- `409 Conflict` - Duplicate resource (e.g., ISBN already exists)

### Authentication

```http
POST /api/register                          # Register a new user
POST /api/login                             # Login with username/password (form data)
POST /api/logout                            # Logout current user
GET  /api/whoami                            # Check current authentication status
```

#### User Roles

- `ROLE_LIBRARIAN` - Full access to manage everything
- `ROLE_CUSTOMER` - Limited access for browsing and reservations

### Public Endpoints (No Authentication Required)

```http
GET  /api/books                             # List all books (paginated)
GET  /api/books/{isbn}                      # Get book by ISBN
GET  /api/books/search                      # Search books (paginated)
GET  /api/authors                           # List all authors (paginated)
GET  /api/authors/{name}                    # Get author by name
GET  /api/books/{isbn}/count                # Count available copies
```

### Admin Endpoints (Librarian Only)

#### Book Management

```http
POST   /api/admin/books                     # Create book
PUT    /api/admin/books/{isbn}              # Update book
DELETE /api/admin/books/{isbn}              # Delete book
```

#### Copy Management

```http
GET    /api/admin/copies                    # List all copies (paginated)
GET    /api/admin/copies/{id}               # Get copy by ID
GET    /api/admin/copies/book/{isbn}        # Get copies of a book (paginated)
POST   /api/admin/copies                    # Create copies (batch)
```

#### Customer Management

```http
GET    /api/admin/customers                 # List all customers (paginated)
POST   /api/admin/customers                 # Create customer (walk-in)
GET    /api/admin/customers/{id}            # Get customer by ID
PUT    /api/admin/customers/{id}            # Update customer
```

### Desk Operations (Librarian Only)

```http
POST /api/desk/checkout                     # Check out copy to customer
POST /api/desk/return                       # Return a copy
POST /api/desk/mark-lost                    # Mark copy as lost
```

### Customer Self-Service (Customer Only)

```http
GET    /api/reservations/mine               # Get my reservations (paginated)
POST   /api/reservations                    # Reserve a book
DELETE /api/reservations/{copyId}           # Cancel reservation
```

### Search Parameters

- Books: `?title=...&authorName=...&publicationYear=...&isbn=...&page=...&size=...`
- Pagination: `?page=0&size=20` (default size: 20, max: 100)

### Copy Status Values

- `AVAILABLE` - Can be borrowed or reserved
- `BORROWED` - Currently checked out
- `RESERVED` - On hold for a customer
- `LOST` - Missing from inventory

## Example HTTP Requests

The `exampleRequests` directory contains ready-to-use HTTP requests:

```
exampleRequests/
├── admin/
│   ├── books.http        # Book CRUD operations
│   ├── copies.http       # Copy management
│   └── customers.http    # Customer management
├── customer/
│   └── reservations.http # Customer self-service
├── librarian/
│   └── desk.http        # Desk operations
├── public/
│   ├── auth.http        # Authentication flows
│   └── browse.http      # Public browsing
└── complete-workflow.http # Full use case example
```

## Progress

- [x] Basic book CRUD operations
- [x] ISBN regex validation and duplicate prevention
- [x] Many-to-many author-book relationships with author duplicate prevention
- [x] Book search by title, author, year, ISBN
- [x] Copy status tracking (available, borrowed, reserved, lost)
- [x] State transition validation for copies
- [x] Comprehensive test coverage written while procrastinating adding new features (ongoing)
- [x] Customer management
- [x] Borrowing and reservation system with customers
- [x] Authentication and authorization with Spring Security
- [x] Password hashing and secure user management
- [x] Role-based access control (Librarian vs Customer)
- [x] Centralized messages
- [x] Proper separation of admin, desk, and customer endpoints
- [x] PostgreSQL migration from H2
- [x] Docker support
- [x] Application monitoring with Prometheus and Grafana
- [x] Custom dashboards in Grafana
- [ ] Integration with external book databases
- [ ] Deployment
- [ ] Proper API documentation
- [ ] Microcontroller-based book scanner