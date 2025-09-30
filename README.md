[![codecov](https://codecov.io/gh/Salad109/library-management/graph/badge.svg?token=P1LBG9BIJR)](https://codecov.io/gh/Salad109/library-management) [![CI](https://github.com/Salad109/library-management/workflows/CI/badge.svg)](https://github.com/Salad109/library-management/actions)

# Library Management System

A library management system built with Spring Boot, split into public book catalog browsing, customer self-service
reservations, and librarian workflow management.

## Features

- Book management with data validation and author relationships
- Author management with many-to-many relationships to books
- Copy tracking with status management (available, borrowed, reserved, lost)
- Borrowing and reservation system with state transition validation
- Customer management with offline and online account separation
- Role-based access control (Librarian vs Customer vs Public)
- Optimistic locking and retry mechanisms to prevent concurrency issues
- Authentication and authorization with Spring Security and password hashing
- Comprehensive integration and unit testing
- Application monitoring with Prometheus and Grafana
- Detailed API documentation with Swagger
- Docker Compose setup for easy deployment
- Caching book catalog with Caffeine
- Logs in JSON format with MDC
- CI with GitHub Actions
- Frontend for staff operations using Thymeleaf and Bootstrap

## Tech Stack

- Java 21 + Spring Boot 3.5
- JPA/Hibernate with PostgreSQL
- Spring Security for authentication and role-based authorization
- Slf4j + Logback for logging
- Prometheus for metrics collection
- Grafana for monitoring dashboards
- Docker Compose for containerization
- Swagger for API documentation
- Caffeine for caching
- JUnit 5 + MockMvc for testing
- Maven
- Thymeleaf + Bootstrap for frontend

## Starting Guide

```bash
# Clone environment
cp .env.example .env
# Edit .env with your database credentials. For example:
# POSTGRES_USER=library_admin
# POSTGRES_PASSWORD=secret123
# POSTGRES_DB=library_db

# Start all services
docker compose up
```

The application runs on `http://localhost:8080`. API documentation is available via Swagger. Monitoring dashboard is
available via Grafana and Prometheus. Access the staff panel at`http://localhost:8080/register`.

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

## API Overview

Full API Documentation and details are available at http://localhost:8080/swagger-ui/index.html. The API is organized
into four main areas:

### Public Browsing (No Authentication Required)

Browse books and authors, search the catalog, check what's available

### Customer Self-Service (CUSTOMER role)

Create, view and cancel your reservations

### Desk Operations (LIBRARIAN role)

Handle checkouts, returns, and mark items as lost

### Admin Operations (LIBRARIAN role)

Manage books, copies, and customer accounts

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
- [x] Book search
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
- [x] Custom dashboard in Grafana
- [x] Proper API documentation
- [x] Caching
- [x] Logs in JSON format with MDC
- [x] CI
- [x] Login and registration pages
- [x] Admin panel with full operations:
    - [x] Books
    - [x] Copies
    - [x] Customers
- [x] Concurrency protection
- [ ] Enhance admin panel with HTMX
- [ ] Deployment

## Gallery

Book browsing page
![](https://github.com/user-attachments/assets/de1e5a27-031c-4ee2-b841-a2b0b95eb057)
Book adding form showcasing data validation
![](https://github.com/user-attachments/assets/8cbd7744-e983-46ee-816b-94e2f8ed8681)
Copy browsing page
![](https://github.com/user-attachments/assets/38449a6c-95f3-45a6-9a75-de6939fff8a4)
Copy checkout form
![](https://github.com/user-attachments/assets/44070f8f-1ade-4554-8b98-d986bacba02f)
Customer browsing page
![](https://github.com/user-attachments/assets/e55750aa-2dd4-4edf-8680-38cef5afc4cc)
Grafana dashboard during high load simulation using the config in `grafana/dashboard.json`.
![](https://github.com/user-attachments/assets/4735967b-be79-4f21-970d-33bcfbd1bebf)
