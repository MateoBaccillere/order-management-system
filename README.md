# Order Management System

A backend MVP for managing customer orders, product catalog validation, and cart checkout flow, built as part of a weekly backend practice project focused on Java, Spring Boot, SQL, Docker, Jenkins, and disciplined software delivery.

## Overview

This project simulates a business order flow where customers can build a cart, validate products against a catalog, generate an order through checkout, and then continue the order lifecycle through explicit status transitions.

At the current stage, the system includes:

- an `order-service` as the core business service
- a `product-service` as the source of truth for catalog data
- a lightweight `notification-service` for order event notifications
- PostgreSQL persistence
- Docker-based local environment
- Jenkins pipeline for build, test, and packaging automation

## Goals

The main goals of this MVP are:

- practice real-world backend development with Java and Spring Boot
- model business rules explicitly and incrementally
- expose clean REST APIs
- integrate services with simple synchronous communication
- keep the architecture realistic but not overengineered
- containerize the system with Docker
- automate validation with Jenkins
- evolve weekly MVPs into portfolio-quality backend systems

## Current Project Stage

The project is not in initial design stage anymore.

Current state:

- `order-service` is already implemented as the core of the system
- `product-service` is already implemented and integrated with `order-service`
- `notification-service` already exists as a lightweight support service
- cart functionality was implemented **inside `order-service`**
- Docker Compose and Jenkins are already part of the working setup
- Week 1 is focused on closing the operational and documented version of this integrated baseline

## Architecture

The system is currently composed of three services.

### 1. order-service
Responsible for:

- creating orders
- retrieving orders
- handling order status transitions
- calculating totals
- persisting orders in PostgreSQL
- managing cart operations
- validating products through `product-service`
- creating orders from validated product snapshots
- sending notifications to `notification-service`

### 2. product-service
Responsible for:

- creating products
- retrieving products
- listing products
- updating stock
- activating and deactivating products
- enforcing product business rules
- acting as the source of truth for product name, price, stock, and active status

### 3. notification-service
Responsible for:

- receiving order event notifications
- storing notifications in memory
- exposing notifications for inspection

## Service Communication

Current service communication is synchronous over HTTP:

- `order-service` calls `product-service` to validate products before direct order creation or checkout
- `order-service` calls `notification-service` when relevant order events occur

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
- H2 for tests

## Project Structure

```text
order-management-system/
├── README.md
├── docker-compose.yml
├── Jenkinsfile
├── order-service/
├── product-service/
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
- `productId`
- `productName`
- `quantity`
- `unitPrice`
- `subtotal`

`productName` and `unitPrice` are stored as snapshots inside the order to preserve historical purchase data, while `product-service` remains the catalog source of truth.

## Cart
Represents the current shopping cart associated with a customer.

### Main fields
- `id`
- `customerName`
- `items`

## CartItem
Represents an item inside a cart.

### Main fields
- `id`
- `productId`
- `quantity`

# Order Statuses
- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

# Business Rules Summary

## Product rules
- product name must not be blank
- product price must be greater than zero
- stock must not be negative
- active must not be null
- duplicate product names are not allowed

## Order rules
- a new order starts with status `CREATED`
- an order must contain at least one item
- item quantity must be greater than zero
- order total is calculated from all order items
- products are validated through `product-service`
- product must exist
- product must be active
- product must have enough stock before order creation

## Order status transition rules
- `CREATED -> CONFIRMED` is allowed
- `CREATED -> CANCELLED` is allowed
- `CONFIRMED -> SHIPPED` is allowed
- `CONFIRMED -> CANCELLED` is allowed
- `CANCELLED` is a terminal state
- `SHIPPED` is a terminal state

## Cart rules
- cart is implemented inside `order-service`
- cart is currently associated with `customerName`
- if the customer has no cart, one is created on demand
- cart item quantity must be greater than zero
- checkout revalidates all products against the catalog
- checkout creates an order in `CREATED`
- cart is cleared after a successful checkout
- cart is ephemeral and does not keep historical states

# API Endpoints

## Product Service

### Create product
`POST /api/products`

### Get product by id
`GET /api/products/{id}`

### Get all products
`GET /api/products`

### Update stock
`PATCH /api/products/{id}/stock`

### Activate product
`PATCH /api/products/{id}/activate`

### Deactivate product
`PATCH /api/products/{id}/deactivate`

## Order Service

### Create order directly
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

## Cart Endpoints

### Get cart
`GET /api/cart/{customerName}`

### Add item to cart
`POST /api/cart/{customerName}/items`

### Update cart item quantity
`PATCH /api/cart/{customerName}/items/{productId}`

### Remove item from cart
`DELETE /api/cart/{customerName}/items/{productId}`

### Clear cart
`DELETE /api/cart/{customerName}`

### Checkout cart
`POST /api/cart/{customerName}/checkout`

## Notification Service

### Create notification
`POST /api/notifications`

### Get all notifications
`GET /api/notifications`

# Example Requests

## Create Product
```json
{
"name": "Mechanical Keyboard",
"price": 120.00,
"stock": 10,
"active": true
}
```
## Create Order Directly
```json
{
"customerName": "Juan Perez",
"items": [{
    "productId": 1,
    "quantity": 2
    }
  ]
}
```
## Add Item to Cart
```json
{
"productId": 1,
"quantity": 2
}
```
# Example Flows

## Direct order flow
1. Create a product in `product-service`
2. Create an order in `order-service` using `productId` and `quantity`
3. `order-service` validates the product through `product-service`
4. Order is created in `CREATED`
5. Confirm the order
6. Ship the order
7. Check notifications in `notification-service`

## Cart checkout flow
1. Create a product in `product-service`
2. Add item to cart in `order-service`
3. Update quantity if needed
4. Execute checkout
5. `order-service` revalidates products through `product-service`
6. Order is created in `CREATED`
7. Cart is cleared
8. Continue normal order lifecycle

## Alternative cancellation flow
1. Create an order
2. Confirm it or keep it in `CREATED`
3. Cancel the order according to allowed transitions
4. Check notifications in `notification-service`

# Running Locally

## Requirements
- Java 21
- Maven or Maven Wrapper
- Docker Desktop
- PostgreSQL if running manually outside Docker
- Jenkins (optional, for CI practice)

## Running Services Manually

### 1. Run `notification-service`
From its folder:
```bash
mvnw.cmd spring-boot:run
```
### 2. Run `product-service`
From its folder:
```bash
mvnw.cmd spring-boot:run
```
### 3. Run `order-service`
From its folder:
```bash
mvnw.cmd spring-boot:run
```
Make sure PostgreSQL is running and that inter-service URLs are configured correctly.

## Running with Docker Compose

From the project root:

```bash
docker compose up --build
```
This starts:

- PostgreSQL
- `notification-service`
- `product-service`
- `order-service`

# Running Tests

## Product service
From the `product-service` folder:

```bash
mvnw.cmd test
```
## Order Service
From the `order-service` folder
```bash
mvnw.cmd test
```
# Jenkins Pipeline

The project includes a `Jenkinsfile` with a simple CI pipeline that performs:

- checkout
- build
- test
- package

At the current stage, the pipeline validates both:

- `product-service`
- `order-service`

The pipeline is intentionally simple and focused on reproducible MVP delivery.

# Current MVP Scope

This version currently includes:

- product creation and management
- product stock updates
- product activation and deactivation
- order creation using catalog validation
- order retrieval
- order status transitions
- cart management inside `order-service`
- checkout flow from cart to order
- notification sending between services
- PostgreSQL persistence
- Docker setup
- Jenkins pipeline
- service-layer and controller-level tests for the implemented flows

# Limitations

This MVP keeps some things intentionally simple:

- cart is associated with `customerName` because there is no `user-service` yet
- notification data is stored in memory
- no authentication or authorization yet
- no pagination or filtering
- no retry or resilience mechanism for inter-service communication
- no event broker such as Kafka or RabbitMQ
- no frontend
- shared PostgreSQL setup is used for MVP simplicity

# Planned Next Step

According to the execution plan, the next service to be implemented after Week 1 is:

- `shipping-service`

Planned focus for the next stage:

- shipment creation rules
- shipment lifecycle
- assignment of confirmed orders to shipment flow
- basic integration with `order-service`

# Future Improvements

- add `shipping-service`
- add `user-service`
- add simple security with JWT or Basic Auth
- persist notifications in a database
- add stronger integration tests
- add OpenAPI/Swagger documentation
- add filtering and pagination later
- improve CI/CD pipeline incrementally
- add Flyway or Liquibase for database migrations

# Learning Focus

This project was built to practice:

- backend design with Spring Boot
- business rules modeling
- service-to-service communication
- SQL persistence
- incremental microservice growth
- Docker-based local environments
- Jenkins pipeline setup
- production-style backend documentation
- disciplined weekly MVP execution

# Author

Built as part of a daily MVP practice system for backend portfolio development.