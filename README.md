# Order Management System

A backend MVP for managing customer orders, built to practice real-world backend development with Java, Spring Boot, SQL, Docker, and Jenkins.

## Goal

This project simulates a basic business order flow where orders can be created, queried, confirmed, cancelled, and shipped.

It is designed as a weekly MVP with:
- a core `order-service`
- a secondary `notification-service`
- SQL persistence
- Docker-based local execution
- a basic Jenkins pipeline
- technical documentation for portfolio purposes

## MVP Scope

This MVP includes:

- Create an order with one or more items
- Retrieve orders by id
- List orders
- Confirm an order
- Cancel an order
- Ship an order
- Validate allowed order status transitions
- Calculate total order amount from items
- Simulate notifications when important order events happen

## Tech Stack

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- SQL database
- Docker
- Docker Compose
- Jenkins
- Git

## Services

### 1. order-service
Responsible for:
- order creation
- order retrieval
- business rules
- order status transitions
- persistence

### 2. notification-service
Responsible for:
- receiving simple event notifications
- logging or exposing received notifications
- simulating cross-service communication

## Core Domain

### Order
Main aggregate representing a customer order.

Suggested fields:
- id
- customerName
- status
- totalAmount
- createdAt
- updatedAt

### OrderItem
Represents an item inside an order.

Suggested fields:
- id
- orderId
- productName
- quantity
- unitPrice
- subtotal

## Order Statuses

- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

## Business Rules

- A new order starts with status `CREATED`
- `CREATED` -> `CONFIRMED` is allowed
- `CREATED` -> `CANCELLED` is allowed
- `CONFIRMED` -> `SHIPPED` is allowed
- `CONFIRMED` -> `CANCELLED` is allowed
- `CANCELLED` -> any other state is not allowed
- `SHIPPED` -> any other state is not allowed
- Order total must be calculated from its items
- An order must contain at least one item
- Item quantity must be greater than zero
- Item unit price must be greater than zero

## Proposed API Endpoints

### Orders
- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{id}`
- `POST /api/orders/{id}/confirm`
- `POST /api/orders/{id}/cancel`
- `POST /api/orders/{id}/ship`

### Notification Service
- `POST /api/notifications`
- `GET /api/notifications`

## Project Structure

```text
order-management-system/
├── README.md
├── docs/
│   ├── architecture.md
│   ├── business-rules.md
│   ├── backlog.md
│   └── dev-notes.md
├── order-service/
├── notification-service/
├── docker-compose.yml
└── Jenkinsfile


