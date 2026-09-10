# Backend Milestone 1 — Spring Boot Architecture & Setup

## Overview

Backend Milestone 1 establishes the foundational architecture for the ShopStack Enterprise Multi-Vendor E-Commerce Platform.

## Architecture Pattern: Controller → Service → Repository → Database

```
  [ Client (React Frontend / Postman) ]
                   │
                   ▼ (HTTP GET /api/health)
      [ Controller Layer (HealthController) ]
                   │
                   ▼ (Java Method Call)
      [ Service Layer (HealthService) ]
                   │
                   ▼ (DTO Returned)
      [ Controller serializes DTO to JSON ]
                   │
                   ▼ (HTTP 200 OK Response)
               [ Client ]
```

### Layer Responsibilities Explained (Beginner Friendly)

1. **Controller Layer (`com.shopstack.controller`)**:
   - Accepts incoming HTTP requests.
   - Maps URLs and HTTP methods (`GET`, `POST`, etc.).
   - Converts HTTP request parameters/JSON body into Java objects.
   - Delegates business operations to the Service layer.
   - Returns structured HTTP responses with proper HTTP status codes (200 OK, 400 Bad Request, etc.).

2. **Service Layer (`com.shopstack.service`)**:
   - Encapsulates business logic and workflow rules.
   - Coordinates repository calls and calculations.
   - Remains independent of HTTP frameworks or web controllers.

3. **Repository Layer (`com.shopstack.repository`)**:
   - Manages persistence operations against PostgreSQL using Spring Data JPA & Hibernate.
   - Executes SQL queries or ORM methods to fetch, save, update, or delete data.

4. **Database (`PostgreSQL`)**:
   - Stores persistent application state (`shopstack_db`).

---

## Health Check API Endpoint

- **URL**: `GET http://localhost:8080/api/health`
- **Response Format**: `application/json`
- **HTTP Status**: `200 OK`
- **Response Payload**:

```json
{
  "status": "UP",
  "message": "ShopStack backend is running"
}
```
