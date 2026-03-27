# Architecture

## Overview

The system is currently composed of three backend services:

- `order-service`
- `product-service`
- `notification-service`

The architecture keeps `order-service` as the core business service while introducing `product-service` as the source of truth for catalog data and keeping `notification-service` as a lightweight support service.

The goal at this stage is to preserve a realistic microservice direction without overengineering the MVP.

## Current Architectural Stage

The project is no longer in initial design stage.

Current state:

- `order-service` is already implemented as the core of the system
- `product-service` is already implemented and integrated
- `notification-service` already exists as a lightweight support service
- cart functionality was implemented inside `order-service`
- Docker Compose and Jenkins already support the current MVP baseline

This means the project is in active execution over an existing functional architecture, not in architecture discovery.

## Services

### 1. order-service

The `order-service` is the core service of the system.

Responsibilities:

- create orders
- retrieve orders
- manage order status transitions
- validate business rules
- calculate order totals
- persist orders and items in PostgreSQL
- manage cart operations
- validate products through `product-service`
- create orders from validated product snapshots
- notify `notification-service` when relevant order events happen

It remains the central service because it owns the order lifecycle and coordinates the main purchase flow.

### 2. product-service

The `product-service` is a standalone catalog service.

Responsibilities:

- create products
- retrieve products by id
- list products
- update stock
- activate products
- deactivate products
- enforce product business rules
- act as the source of truth for:
  - product name
  - product price
  - product stock
  - product active status

This service was introduced so product validation and catalog rules do not stay mixed inside `order-service`.

### 3. notification-service

The `notification-service` is a lightweight supporting service.

Responsibilities:

- receive notifications from `order-service`
- store notifications in memory
- expose stored notifications through an API

At this stage, it remains intentionally simple.

## Communication

The services communicate synchronously through HTTP.

### Current service interactions

- `order-service` calls `product-service` to validate products before:
  - direct order creation
  - cart operations when needed
  - checkout
- `order-service` calls `notification-service` when relevant order events occur

This synchronous approach is intentionally simple and appropriate for the MVP stage.

## Current Functional Flow

### Direct order flow

1. A client sends an order creation request to `order-service`
2. The request includes `customerName`, `productId`, and `quantity`
3. `order-service` queries `product-service`
4. `product-service` returns catalog data
5. `order-service` validates:
   - product existence
   - product active status
   - stock availability
6. `order-service` stores the order and order items
7. The order is created in `CREATED`
8. `order-service` can later notify `notification-service` on relevant order events

### Cart checkout flow

1. A client interacts with cart endpoints in `order-service`
2. The cart is associated with `customerName`
3. Cart items store `productId` and `quantity`
4. On checkout, `order-service` revalidates all products against `product-service`
5. `order-service` creates an order in `CREATED`
6. The cart is cleared after successful checkout
7. The new order continues the normal order lifecycle

## Architectural Style

Each service follows a simple layered architecture.

This keeps the codebase understandable, testable, and appropriate for incremental weekly delivery.

## Layered Design

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
  Contains HTTP clients used to communicate with external services

- `config`  
  Contains application configuration classes

### Important internal application services in order-service

At the current stage, `order-service` was internally refactored to separate responsibilities more clearly:

- `CatalogProductValidationService`  
  Centralizes validation against `product-service`

- `OrderCreationService`  
  Centralizes order construction and persistence from validated product data

- `OrderService`  
  Coordinates direct order use cases and order lifecycle transitions

- `CartService`  
  Handles cart retrieval, mutation, and checkout flow

This is an internal modularization improvement without breaking the current service boundary.

### product-service layers

- `controller`
- `dto`
- `entity`
- `repository`
- `service`
- `exception`

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
- `productId`
- `productName`
- `quantity`
- `unitPrice`
- `subtotal`
- `order`

`productName` and `unitPrice` are stored as snapshots to preserve historical purchase data even if the catalog changes later.

### Cart

Main fields:

- `id`
- `customerName`
- `items`

### CartItem

Main fields:

- `id`
- `productId`
- `quantity`
- `cart`

### Product

Main fields:

- `id`
- `name`
- `price`
- `stock`
- `active`

## Status Model

### Order statuses

- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

### Status transition rules

Allowed transitions:

- `CREATED -> CONFIRMED`
- `CREATED -> CANCELLED`
- `CONFIRMED -> SHIPPED`
- `CONFIRMED -> CANCELLED`

Terminal states:

- `CANCELLED`
- `SHIPPED`

## Persistence

### order-service
Uses PostgreSQL for persistence of:

- orders
- order items
- carts
- cart items

### product-service
Uses PostgreSQL for persistence of:

- products

### notification-service
Stores data in memory for now.

This decision keeps the support service lightweight while persistence effort is focused on the core business flows.

## Containerization

The system includes a Docker Compose setup that starts:

- PostgreSQL
- `product-service`
- `order-service`
- `notification-service`

This allows local execution of the real Week 1 integrated baseline with a single command.

## Continuous Integration

A Jenkins pipeline is included to automate:

- checkout
- build
- test
- package

At the current stage, the pipeline validates:

- `product-service`
- `order-service`

This keeps CI aligned with the real functional scope of Week 1.

## Design Decisions

### Why keep order-service as the core?
Because the main domain of the project is still the order lifecycle. Even after adding catalog validation and cart functionality, the central business orchestration remains in `order-service`.

### Why create product-service?
Because product rules, stock checks, and active/inactive logic belong to catalog management and should not remain duplicated or hardcoded inside order logic.

### Why keep cart inside order-service?
Because introducing a separate cart microservice in Week 1 would add unnecessary complexity too early. At this stage, cart is part of the purchase flow and can live inside the core service.

### Why keep synchronous HTTP instead of messaging?
Because it is simpler to implement, debug, and validate for an MVP that is still focused on clear business flow execution.

### Why store product snapshots in OrderItem?
Because an order must preserve the historical context of what was purchased, even if the product catalog later changes.

### Why keep notification-service simple?
Because notifications are currently a support capability, not the central business domain.

## Tradeoffs

Current tradeoffs accepted for the MVP:

- cart is associated with `customerName` because `user-service` does not exist yet
- synchronous communication means tighter runtime coupling between services
- notification persistence is still in memory
- a single PostgreSQL setup is used for practical MVP execution
- resilience patterns such as retries or circuit breakers are not yet implemented

These are acceptable at the current stage and should not be overengineered early.

## Future Architecture Improvements

Planned or possible future improvements include:

- add `shipping-service` in Week 2
- add `user-service` in Week 3
- introduce simple authentication in Week 4
- persist notifications in a database
- improve integration testing
- add OpenAPI documentation
- add database migrations
- improve resilience for service-to-service calls
- add observability and structured logging