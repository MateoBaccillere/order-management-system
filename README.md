# Order Management System

Backend MVP project built with Java and Spring Boot, designed to evolve incrementally into a portfolio-ready microservices architecture.

The system started with `order-service` as the core domain and has been expanded week by week with new business capabilities while preserving a simple, realistic and maintainable structure.

At the current stage, after the completion of Week 2, the system already supports:

- product management through `product-service`
- order creation and lifecycle through `order-service`
- cart management inside `order-service`
- shipment lifecycle through `shipping-service`
- notifications through `notification-service`
- PostgreSQL persistence
- Docker Compose integration
- Jenkins CI validation

---

## Current Architecture

The system currently includes four services:

### 1. `product-service`
Responsible for:

- product creation
- product updates
- stock handling
- active/inactive product state
- catalog validation source for order creation and checkout

### 2. `order-service`
Core transactional service responsible for:

- cart management
- checkout
- order creation
- order total calculation
- order persistence
- order lifecycle transitions
- product snapshot storage
- integration with `product-service`
- integration with `notification-service`

### 3. `shipping-service`
Introduced in Week 2 to handle:

- shipment creation
- shipment lifecycle transitions
- shipment persistence
- order validation before shipment creation
- notification of shipment events
- delivery-driven order shipping completion

### 4. `notification-service`
Lightweight support service that receives notification requests from other services.

---

## Current Business Flow

The implemented happy path is:

1. seller creates and activates products
2. buyer browses active products
3. buyer adds items to cart
4. buyer performs checkout
5. `order-service` creates an order with status `CREATED`
6. seller or admin confirms the order
7. `shipping-service` creates a shipment for the confirmed order
8. shipment moves through:
   - `PENDING`
   - `READY_FOR_DELIVERY`
   - `IN_TRANSIT`
   - `DELIVERED`
9. when shipment reaches `DELIVERED`, `shipping-service` calls `order-service`
10. `order-service` transitions the related order to `SHIPPED`
11. order and shipment events are sent to `notification-service`

### Important rule
An order does **not** become `SHIPPED` directly after confirmation.  
It becomes `SHIPPED` only when the related shipment reaches `DELIVERED`.

---

## Order Lifecycle

The official order statuses are:

- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

Allowed transitions:

- `CREATED -> CONFIRMED`
- `CREATED -> CANCELLED`
- `CONFIRMED -> CANCELLED`
- `CONFIRMED -> SHIPPED`

### Official interpretation
`CONFIRMED -> SHIPPED` is no longer treated as an isolated manual business step.

It now represents the order completion triggered by shipment delivery confirmation from `shipping-service`.

---

## Shipment Lifecycle

The official shipment statuses are:

- `PENDING`
- `READY_FOR_DELIVERY`
- `IN_TRANSIT`
- `DELIVERED`
- `FAILED`
- `CANCELLED`

Allowed transitions:

- `PENDING -> READY_FOR_DELIVERY`
- `READY_FOR_DELIVERY -> IN_TRANSIT`
- `IN_TRANSIT -> DELIVERED`
- `IN_TRANSIT -> FAILED`
- `PENDING -> CANCELLED`
- `READY_FOR_DELIVERY -> CANCELLED`

Shipment transitions are exposed through command-style `PATCH` endpoints.

Examples:

- `/api/shipments/{id}/ready`
- `/api/shipments/{id}/in-transit`
- `/api/shipments/{id}/deliver`
- `/api/shipments/{id}/fail`
- `/api/shipments/{id}/cancel`

---

## Product Validation and Order Snapshots

`product-service` is the source of truth for:

- product existence
- current price
- active status
- available stock

Before creating an order, `order-service` validates products through `product-service`.

After validation, `order-service` stores a purchase snapshot inside each `OrderItem`, including:

- `productId`
- product name at purchase time
- product price at purchase time
- ordered quantity

This prevents future product changes from affecting historical orders.

---

## Cart Design

The cart is intentionally implemented inside `order-service`.

This was a deliberate architectural decision to keep the MVP simple and avoid introducing a separate cart microservice too early.

The cart supports:

- add item
- list cart
- update quantity
- remove item
- clear cart
- checkout

At the current stage, cart ownership is temporarily associated with `customerName` until `user-service` is introduced.

---

## Roles and Operational Ownership

The project already assumes these business roles:

- `BUYER`
- `SELLER`
- `ADMIN`

Formal role enforcement is planned for Week 3, but the system design already follows these ownership rules:

### `BUYER`
Can:

- browse active products
- manage cart
- checkout
- view own orders
- view own shipment status

Cannot:

- change product states
- change order states
- change shipment states

### `SELLER`
Can:

- manage owned products
- manage owned operational order transitions
- create and manage shipments for owned orders

### `ADMIN`
Can:

- manage all operational resources
- perform global overrides when needed

---

## Notification Model

Each service emits notifications for its own domain events.

### `order-service`
Notifies events such as:

- `ORDER_CONFIRMED`
- `ORDER_CANCELLED`
- `ORDER_SHIPPED`

### `shipping-service`
Notifies events such as:

- `SHIPMENT_CREATED`
- `SHIPMENT_READY_FOR_DELIVERY`
- `SHIPMENT_IN_TRANSIT`
- `SHIPMENT_DELIVERED`
- `SHIPMENT_FAILED`
- `SHIPMENT_CANCELLED`

`notification-service` receives and processes those requests but does not own domain rules.

---

## Infrastructure

The current infrastructure keeps the project intentionally simple and reproducible.

### Database
A single shared PostgreSQL instance is used at this stage.

### Communication
Services communicate synchronously through HTTP.

### Docker
Docker Compose currently includes:

- `postgres`
- `notification-service`
- `product-service`
- `order-service`
- `shipping-service`

### CI
Jenkins currently validates the main services with:

- build
- test
- package

for:

- `product-service`
- `order-service`
- `shipping-service`

---

## Project Structure

Current repository-level structure is conceptually organized around:

- `product-service`
- `order-service`
- `shipping-service`
- `notification-service`
- infrastructure and documentation files

The exact internal folder depth may differ by service, but each service remains independently buildable.

---

## Technology Stack

- Java 17
- Spring Boot
- Spring Data JPA
- PostgreSQL
- H2 for tests
- Docker Compose
- Jenkins
- Maven

---

## Current Project Status

### Week 1 completed
Week 1 closed with:

- `product-service`
- product validation integrated into `order-service`
- cart implementation inside `order-service`
- Docker Compose aligned with the integrated baseline
- Jenkins validating `product-service` and `order-service`

### Week 2 completed
Week 2 closed with:

- `shipping-service` implemented as a standalone microservice
- shipment lifecycle and command-style `PATCH` endpoints
- shipment creation restricted to confirmed orders
- `DELIVERED -> SHIPPED` integration with `order-service`
- shipment notifications
- Docker Compose updated with `shipping-service`
- Jenkins updated to validate `shipping-service`

---

## Current Architectural Direction

The project intentionally favors:

- incremental weekly delivery
- realistic service boundaries
- maintainability
- visible portfolio value
- minimal reproducible DevOps
- business-rule clarity
- no premature distributed-system complexity

The project intentionally avoids, for now:

- Kafka
- Kubernetes
- OAuth
- split shipment workflows
- advanced orchestration
- complex deployment automation

---

## Next Planned Step

The next planned growth is **Week 3**:

- `user-service`
- explicit `BUYER` / `SELLER` / `ADMIN` modeling
- ownership enforcement across products, orders and shipments

This is the natural next step because the system already has enough business flow to justify formal identity and authorization rules.