# Architecture

## Overview

The system is composed of two backend services:

- `order-service`
- `notification-service`

The goal of the architecture is to keep the MVP simple while still showing service separation, business rules enforcement, persistence, and basic inter-service communication.

## Services

### order-service

The `order-service` is the core service of the system.

Responsibilities:

- create orders
- retrieve orders
- manage order status transitions
- validate business rules
- calculate order totals
- persist orders and items in PostgreSQL
- notify the notification service when relevant order events happen

### notification-service

The `notification-service` is a lightweight supporting service.

Responsibilities:

- receive notifications from the order service
- store notifications in memory
- expose stored notifications through an API

## Communication

The services communicate synchronously through HTTP.

### Current flow

1. A client sends a request to the `order-service`
2. The `order-service` processes the request
3. If the order state changes, the `order-service` sends an HTTP request to the `notification-service`
4. The `notification-service` stores the notification and returns a response

This approach is intentionally simple for the MVP.

## Architectural Style

This project follows a simple layered architecture inside each service.

### order-service layers

- `controller`  
  Handles HTTP requests and responses

- `dto`  
  Contains request and response payloads

- `entity`  
  Contains JPA entities and domain enums

- `repository`  
  Handles persistence operations

- `service`  
  Contains business logic and use cases

- `exception`  
  Contains custom exceptions and global exception handling

- `client`  
  Contains the HTTP client used to communicate with the notification service

- `config`  
  Contains application configuration classes

### notification-service layers

- `controller`
- `dto`
- `service`
- `config`

## Data Model

### Order

Main fields:

- `id`
- `customerName`
- `status`
- `totalAmount`
- `createdAt`
- `updatedAt`

### OrderItem

Main fields:

- `id`
- `productName`
- `quantity`
- `unitPrice`
- `subtotal`
- `order`

## Status Model

Possible order statuses:

- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

## Status Transition Rules

Allowed transitions:

- `CREATED -> CONFIRMED`
- `CREATED -> CANCELLED`
- `CONFIRMED -> SHIPPED`
- `CONFIRMED -> CANCELLED`

Terminal states:

- `CANCELLED`
- `SHIPPED`

## Persistence

The `order-service` uses PostgreSQL for persistence.

The `notification-service` stores data in memory for now, since persistence is not required for the first MVP version.

## Containerization

The system includes a Docker Compose setup that starts:

- PostgreSQL
- `order-service`
- `notification-service`

This allows local multi-service execution with a single command.

## Continuous Integration

A Jenkins pipeline is included to automate:

- checkout
- build
- test
- package

This gives the project a basic CI flow suitable for portfolio and local practice.

## Design Decisions

### Why separate notification-service?
To practice basic microservice boundaries and service-to-service communication without introducing unnecessary infrastructure complexity.

### Why synchronous HTTP instead of messaging?
Because it is simpler to implement and debug for a first-week MVP.

### Why PostgreSQL only in order-service?
Because order persistence is part of the core business domain, while notifications are currently just a support feature.

### Why explicit status transition endpoints?
Because they express domain intent clearly and help enforce business rules at the API level.

## Future Architecture Improvements

- persist notifications in a database
- replace synchronous communication with asynchronous events
- add retry/fallback handling for inter-service calls
- add OpenAPI documentation
- add database migrations
- add observability and structured logging