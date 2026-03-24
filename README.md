# Order Management System

A backend MVP for managing customer orders, built as part of a weekly practice project focused on backend development, microservices, Docker, Jenkins, and software delivery discipline.

## Overview

This project simulates a basic business order flow where orders can be created, retrieved, and updated through explicit status transitions.

It includes:

- an `order-service` responsible for order management and business rules
- a `notification-service` responsible for receiving and storing simple order event notifications
- SQL persistence for orders
- Docker-based local setup
- a Jenkins pipeline for build, test, and packaging automation

## Goals

The main goals of this MVP are:

- practice real-world backend development with Java and Spring Boot
- model business rules clearly
- expose a clean REST API
- implement simple service-to-service communication
- containerize the system with Docker
- automate validation with Jenkins

## Architecture

The system is composed of two services:

### 1. order-service
Responsible for:

- creating orders
- retrieving orders
- calculating order totals
- handling order status transitions
- persisting order data in PostgreSQL
- sending notifications to the notification service

### 2. notification-service
Responsible for:

- receiving order event notifications
- storing notifications in memory
- exposing notifications for inspection

### Communication
The `order-service` communicates with the `notification-service` through synchronous HTTP calls.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Docker
- Docker Compose
- Jenkins
- JUnit 5
- Mockito
- Maven

## Project Structure

```text
order-management-system/
├── README.md
├── docs/
│   ├── architecture.md
│   ├── business-rules.md
│   ├── backlog.md
│   └── dev-notes.md
├── docker-compose.yml
├── Jenkinsfile
├── order-service/
└── notification-service/
```
# Core Domain

## Order

Represents a customer order.

### Main fields
- `id`
- `customerName`
- `status`
- `totalAmount`
- `createdAt`
- `updatedAt`

## OrderItem

Represents an item inside an order.

### Main fields
- `id`
- `productName`
- `quantity`
- `unitPrice`
- `subtotal`

## Order Statuses
- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

## Business Rules
- A new order starts with status `CREATED`
- An order must contain at least one item
- Item quantity must be greater than zero
- Item unit price must be greater than zero
- The total amount is calculated from all order items
- `CREATED -> CONFIRMED` is allowed
- `CREATED -> CANCELLED` is allowed
- `CONFIRMED -> SHIPPED` is allowed
- `CONFIRMED -> CANCELLED` is allowed
- `CANCELLED` is a terminal state
- `SHIPPED` is a terminal state

# API Endpoints

## Order Service

### Create order
`POST /api/orders`

### Get all orders
`GET /api/orders`

### Get order by id
`GET /api/orders/{id}`

### Confirm order
`PATCH /api/orders/{id}/confirm`

### Cancel order
`PATCH /api/orders/{id}/cancel`

### Ship order
`PATCH /api/orders/{id}/ship`

## Notification Service

### Create notification
`POST /api/notifications`

### Get all notifications
`GET /api/notifications`

## Example Request

### Create Order

```json
{
  "customerName": "Juan Perez",
  "items": [
    {
      "productName": "Keyboard",
      "quantity": 2,
      "unitPrice": 50.00
    },
    {
      "productName": "Mouse",
      "quantity": 1,
      "unitPrice": 25.00
    }
  ]
}
```
## Example Flow

### Main flow
1. Create an order
2. Confirm the order
3. Ship the order
4. Check notifications in the notification service

### Alternative flow
1. Create an order
2. Cancel the order
3. Check notifications in the notification service

## Running Locally

### Requirements
- Java 21
- Maven or Maven Wrapper
- PostgreSQL
- Docker Desktop
- Jenkins (optional, for CI practice)

### Start services manually

#### 1. Run `notification-service`
From its folder:
mvnw.cmd spring-boot:run
#### 2. Run `order-service`
From its folder:
mvnw.cmd spring-boot:run
Make sure PostgreSQL is running and the database configuration is correct.

## Running with Docker Compose

From the project root:
docker compose up --build

This starts:
- PostgreSQL
- `notification-service`
- `order-service`

## Running Tests

From the `order-service` folder:
mvnw.cmd test
## Jenkins Pipeline

The project includes a `Jenkinsfile` with a simple pipeline that performs:
- checkout
- build
- test
- package

This pipeline is intended as a basic CI setup for local Jenkins practice.

## Current MVP Scope

This version includes:
- order creation
- order retrieval
- order status transitions
- business rule validation
- notification sending between services
- Docker setup
- Jenkins pipeline
- basic service-layer tests

## Limitations

This MVP keeps some things intentionally simple:
- notification data is stored in memory
- no authentication or authorization
- no pagination or filtering
- no retry or resilience mechanism for inter-service communication
- no event broker such as Kafka or RabbitMQ
- no frontend

## Future Improvements

- add authentication and authorization
- persist notifications in a database
- add integration tests
- add OpenAPI/Swagger documentation
- add filtering and pagination
- introduce asynchronous communication
- add resilience patterns for service-to-service calls
- add frontend client
- improve CI/CD pipeline
- add Flyway or Liquibase for database migrations

## Learning Focus

This project was built to practice:
- backend design with Spring Boot
- business rules modeling
- REST API design
- SQL persistence
- basic microservice communication
- Docker-based local environments
- Jenkins pipeline setup
- writing production-style project documentation

## Author

Built as part of a daily MVP practice system for backend portfolio development.




