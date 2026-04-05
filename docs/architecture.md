# Architecture

## 1. Overview

The Order Management System is a backend microservices-based MVP designed to evolve incrementally from a portfolio-ready foundation into a more robust distributed system.

At the current stage, after the completion of Week 2, the system includes:

- `product-service` for catalog management
- `order-service` as the core transactional service
- `shipping-service` for shipment lifecycle handling
- `notification-service` as a lightweight supporting service

The architecture intentionally favors:

- simplicity over premature distribution complexity
- clear bounded responsibilities
- synchronous service-to-service communication
- incremental weekly growth
- maintainability and demonstration value for portfolio purposes

The system is no longer in initial design phase.  
It is in active implementation over an already functional architecture.

---

## 2. Current Service Responsibilities

### 2.1 Product Service
`product-service` is responsible for:

- product creation and updates
- active/inactive state
- stock availability
- current product pricing
- catalog validation source for orders and cart checkout

It acts as the source of truth for current product state.

### 2.2 Order Service
`order-service` is the core service of the system.

It is responsible for:

- cart management
- checkout
- order creation
- order persistence
- order item snapshot persistence
- order lifecycle transitions
- total calculation
- integration with `product-service`
- integration with `notification-service`

It remains the source of truth for the order domain.

### 2.3 Shipping Service
`shipping-service` is introduced in Week 2 as a standalone microservice.

It is responsible for:

- shipment creation
- shipment persistence
- shipment lifecycle transitions
- validating shipment creation against order status
- notifying shipment-related events
- informing `order-service` when shipment delivery is completed

It is the source of truth for the shipment domain.

### 2.4 Notification Service
`notification-service` remains intentionally lightweight.

It is responsible for:

- receiving notification requests from other services
- processing notification messages

It does not own order, product or shipment business rules.

---

## 3. Architectural Principles

### 3.1 Incremental growth
The project evolves one service at a time.

Each weekly increment must:

- introduce one meaningful domain capability
- integrate quickly with the existing baseline
- avoid long isolated redesign phases
- close with a testable and demonstrable result

### 3.2 Core-first design
`order-service` is the center of the current system and all new capabilities grow from that base.

`shipping-service` was introduced as an extension of the existing order flow, not as a replacement of the order core.

### 3.3 Avoid premature overengineering
The architecture explicitly avoids introducing advanced distributed patterns too early.

The current MVP does not introduce:

- Kafka
- event brokers
- workflow engines
- Kubernetes
- advanced deployment automation
- split shipment orchestration
- OAuth
- premature caching layers

### 3.4 Implementation-aware architecture
Documentation and infrastructure must reflect the actual implemented system, not an imagined future state.

This rule is especially important because the project evolves weekly and architectural truth must remain synchronized with code, Docker and CI.

---

## 4. Domain Boundaries

### 4.1 Product domain boundary
The product domain belongs to `product-service`.

Other services may consume product data but do not own:

- stock truth
- active state truth
- current product price truth

### 4.2 Order domain boundary
The order domain belongs to `order-service`.

Other services may read or influence order state through controlled integration, but order lifecycle ownership remains in `order-service`.

### 4.3 Shipment domain boundary
The shipment domain belongs to `shipping-service`.

Other services do not own shipment lifecycle transitions.

### 4.4 Notification domain boundary
Notifications are delegated to `notification-service`, but notification meaning remains owned by the service that emits the event.

For example:

- `ORDER_SHIPPED` belongs conceptually to `order-service`
- `SHIPMENT_DELIVERED` belongs conceptually to `shipping-service`

---

## 5. Service Interaction Model

### 5.1 Current integration style
The system uses synchronous HTTP communication between services.

This is an intentional MVP choice.

### 5.2 Current main interactions

#### Product validation
`order-service -> product-service`

`order-service` validates products during:

- direct order creation
- cart checkout

#### Order notifications
`order-service -> notification-service`

`order-service` notifies order lifecycle events.

#### Shipment notifications
`shipping-service -> notification-service`

`shipping-service` notifies shipment lifecycle events.

#### Shipment creation validation
`shipping-service -> order-service`

`shipping-service` validates that a shipment can only be created for an order in `CONFIRMED`.

#### Delivery-driven shipping completion
`shipping-service -> order-service`

When a shipment reaches `DELIVERED`, `shipping-service` calls `order-service` to mark the related order as `SHIPPED`.

### 5.3 Why synchronous HTTP was chosen
This model was kept because it:

- matches current MVP constraints
- reduces infrastructure complexity
- is easy to debug
- is easy to demonstrate
- aligns with the existing Docker Compose setup
- avoids premature distributed-system overhead

### 5.4 Known limitation
Synchronous integration introduces runtime coupling and the possibility of partial completion scenarios.

Example:

- `shipping-service` persists `DELIVERED`
- notification is sent
- call to `order-service` fails
- shipment stays `DELIVERED`
- order remains `CONFIRMED`

This limitation is currently accepted as part of the MVP simplicity strategy.

---

## 6. Order and Shipment Architecture Alignment

### 6.1 Historical context
Before `shipping-service`, order lifecycle simplification allowed `SHIPPED` to be treated as a direct order-side lifecycle action.

That behavior is no longer the architectural baseline.

### 6.2 Current rule
With the introduction of `shipping-service`, order and shipment lifecycles are separated.

The official rule is:

- `shipping-service` owns shipment lifecycle
- `order-service` owns order lifecycle
- order transition to `SHIPPED` happens only after shipment reaches `DELIVERED`

### 6.3 Why this rule was chosen
This model was chosen because it:

- keeps bounded contexts clearer
- improves semantic correctness
- gives `shipping-service` real architectural purpose
- produces a stronger portfolio design than keeping shipping logic fully inside `order-service`

### 6.4 Tradeoff
This design is better semantically, but it introduces tighter runtime dependency between shipment completion and order lifecycle completion.

At Week 2, this tradeoff is acceptable.

---

## 7. Cart Architecture Decision

### 7.1 Current placement
Cart remains inside `order-service`.

### 7.2 Why this decision was kept
A separate cart microservice was intentionally rejected at this stage because it would:

- add a new service too early
- increase infrastructure and coordination complexity
- provide little extra value for the current MVP scope

### 7.3 Architectural judgment
Keeping cart inside `order-service` is the correct decision for the current project stage.

It preserves focus on visible business value while keeping the architecture understandable and realistic.

---

## 8. Role and Ownership Direction

### 8.1 Business-level role model
The architecture already assumes these actors:

- `BUYER`
- `SELLER`
- `ADMIN`

### 8.2 Week 2 status
Formal enforcement is not fully implemented yet, but the architecture already treats role ownership as part of the domain design.

### 8.3 Operational ownership rule
Lifecycle transitions are treated as operational actions.

Therefore:

- `BUYER` does not manage product, order or shipment states
- `SELLER` manages owned operational resources
- `ADMIN` may operate globally

This ownership model becomes implementation focus in Week 3 through `user-service`.

---

## 9. Persistence Strategy

### 9.1 Current database strategy
The current infrastructure uses a single shared PostgreSQL instance.

This is an intentional MVP infrastructure decision.

### 9.2 Why this strategy was kept
A shared instance was kept because it:

- reduces setup complexity
- keeps Docker Compose simple
- accelerates local integration testing
- is sufficient for the current project scale

### 9.3 Architectural note
Even though services share one database instance at infrastructure level, domain ownership is still separated logically by service responsibility.

The current setup should not be interpreted as a final production-grade persistence strategy.

---

## 10. Docker Architecture

### 10.1 Current Compose baseline
Docker Compose now includes:

- `postgres`
- `notification-service`
- `product-service`
- `order-service`
- `shipping-service`

### 10.2 Networking model
Inside Docker Compose, services communicate using Docker service names.

Examples:

- `product-service`
- `order-service`
- `notification-service`
- `postgres`

`localhost` is reserved for local non-containerized execution only.

### 10.3 Why Docker matters in this architecture
Docker Compose acts as the executable representation of the current architecture.

It is not just a deployment convenience; it is the practical validation layer for the current integrated system design.

---

## 11. CI Architecture

### 11.1 Jenkins scope
Jenkins currently validates the main services through build, test and package stages.

At the end of Week 2, the pipeline includes:

- `product-service`
- `order-service`
- `shipping-service`

### 11.2 Why this scope is appropriate
This pipeline scope is enough for the current stage because it validates:

- compilation
- tests
- packaging readiness

without introducing unnecessary CI complexity.

### 11.3 What CI intentionally does not include yet
The current pipeline does not include:

- deployment stages
- Docker image publishing
- integrated Compose-based end-to-end execution
- advanced test orchestration
- parallel stage optimization

This is intentional and aligned with MVP priorities.

---

## 12. Rejected Alternatives

### 12.1 Event-driven shipping completion
Rejected for Week 2 because it would require broker infrastructure, retry handling and additional operational complexity too early.

### 12.2 Polling between services
Rejected because polling would add unnecessary background coordination logic and worse traceability for the current MVP.

### 12.3 Separate cart microservice
Rejected because the cart domain does not justify its own service yet.

### 12.4 Shipping logic fully inside order-service
Rejected because it weakens domain separation and makes the shipping increment less meaningful architecturally.

---

## 13. Current Week 2 Baseline

At the end of Week 2, the architecture baseline is:

- `order-service` is already implemented and remains the core service
- `product-service` is integrated and acts as product truth
- cart remains inside `order-service`
- `shipping-service` is implemented as a standalone service
- shipment lifecycle is separated from order lifecycle
- order shipping completion is driven by shipment delivery
- `notification-service` remains lightweight
- Docker Compose represents the integrated architecture
- Jenkins validates the main services through build, test and package
- the project remains intentionally simple, synchronous and MVP-oriented

---

## 14. Next Architectural Growth

The next planned architectural growth is Week 3:

- `user-service`
- role modeling
- ownership enforcement
- buyer/seller/admin boundaries implemented more formally

This is the next natural step because the current system already has enough business flow to justify explicit identity and authorization rules.