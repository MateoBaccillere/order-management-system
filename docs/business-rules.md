# Business Rules

## 1. Overview

This document defines the official business rules of the Order Management System after the completion of Week 2.

The system is no longer limited to order creation and basic order lifecycle handling. It now includes:

- `product-service` as the catalog source of truth
- `order-service` as the transactional core of the system
- cart management inside `order-service`
- `shipping-service` for shipment lifecycle handling
- `notification-service` as a lightweight support service

The current architecture supports:

- product creation and management
- cart creation and checkout
- order creation and lifecycle transitions
- shipment creation and shipment lifecycle transitions
- notifications for relevant order and shipment events
- synchronous HTTP communication between services
- shared PostgreSQL persistence at infrastructure level
- Docker Compose and Jenkins validation aligned with the implemented system

The business rules below describe the official expected behavior of the system at this stage.

---

## 2. Product Rules

### 2.1 Product ownership
A product is managed by the seller that owns it.  
At the current stage, ownership is a documented business rule and will be formally enforced when `user-service` is introduced in Week 3.

### 2.2 Required product data
A product must include:

- name
- price
- available stock
- active status

### 2.3 Product validity for order creation
A product can only be used to create an order or checkout cart items when:

- the product exists
- the product is active
- the requested quantity is available in stock

### 2.4 Product snapshot rule
`order-service` must not depend on live product data after order creation.

Each `OrderItem` stores a purchase snapshot containing:

- `productId`
- product name at purchase time
- product price at purchase time
- ordered quantity

This guarantees that historical order data remains stable even if the product changes later.

### 2.5 Product state operations
Product state changes are operational actions and are restricted to:

- `SELLER` owner of the product
- `ADMIN`

A `BUYER` cannot:

- create products
- update products
- activate products
- deactivate products
- change stock

---

## 3. Cart Rules

### 3.1 Cart placement
The cart is intentionally implemented inside `order-service`.

This is a deliberate MVP decision to avoid introducing a separate cart microservice too early.

### 3.2 Cart ownership
At the current stage, cart ownership is temporarily associated with `customerName` until `user-service` is introduced.

### 3.3 Cart operations
The cart supports:

- add item
- list cart
- update quantity
- remove item
- clear cart
- checkout

### 3.4 Cart checkout
Checkout performs these steps:

1. revalidate products against `product-service`
2. build order item snapshots
3. create an order with status `CREATED`
4. calculate totals
5. persist the order
6. clear the cart

### 3.5 Cart historical behavior
The cart is an ephemeral working structure.

It does not keep:

- historical versions
- status lifecycle
- audit timeline

That behavior is intentionally deferred to keep the MVP simple.

---

## 4. Order Rules

### 4.1 Order ownership
An order belongs to the buyer who created it and is operationally managed by the responsible seller or admin.

Formal ownership validation is planned for Week 3 with `user-service`.

### 4.2 Required order data
An order must include:

- customer name
- at least one order item
- total amount
- current order status

### 4.3 Order statuses
The official order statuses are:

- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

### 4.4 Order creation rule
An order can be created only if:

- all referenced products exist
- all referenced products are active
- all requested quantities are valid against current stock

### 4.5 Order total calculation rule
The order total is calculated as the sum of all order item subtotals:

`item subtotal = snapshot price * quantity`

The final order total must always come from the stored snapshots, not from current product prices.

### 4.6 Order lifecycle rules
Allowed transitions:

- `CREATED -> CONFIRMED`
- `CREATED -> CANCELLED`
- `CONFIRMED -> CANCELLED`
- `CONFIRMED -> SHIPPED`

Not allowed:

- `CANCELLED` to any other status
- `SHIPPED` to any other status
- direct `CREATED -> SHIPPED`

### 4.7 Meaning of `SHIPPED`
With the introduction of `shipping-service`, `SHIPPED` is no longer treated as an isolated manual order-side step.

Official rule:

An order must transition to `SHIPPED` **only when the related shipment reaches `DELIVERED` in `shipping-service`**.

This means:

- confirming an order does not mark it as shipped
- creating a shipment does not mark it as shipped
- moving a shipment to `IN_TRANSIT` does not mark it as shipped
- only shipment delivery completion triggers order shipping completion

### 4.8 Order shipping idempotency
If an already shipped order receives the same shipping completion request again, the operation must behave safely and return the current shipped state without duplicating the transition.

This is a minimal idempotency rule introduced to support synchronous service integration.

### 4.9 Order state operations
Order state changes are operational actions and are restricted to:

- `SELLER`
- `ADMIN`
- internal system flow where applicable

A `BUYER` cannot:

- confirm orders
- cancel orders operationally
- ship orders
- force order lifecycle transitions

A buyer can create an order through checkout, but this is a customer-facing commercial action, not a direct lifecycle management permission.

---

## 5. Shipment Rules

### 5.1 Shipment ownership
A shipment belongs to an existing order and is managed by the seller responsible for fulfilling that order or by admin.

### 5.2 Shipment creation
A shipment can only be created when:

- the related order exists
- the related order is in `CONFIRMED`
- the order does not already have a shipment assigned

### 5.3 One shipment per order
At the current MVP stage, an order can have only one shipment.

Multiple shipments, split shipments and partial deliveries are out of scope.

### 5.4 Shipment required data
A shipment must include:

- related `orderId`
- customer name
- shipping address
- shipment status
- timestamps

### 5.5 Shipment statuses
The official shipment statuses are:

- `PENDING`
- `READY_FOR_DELIVERY`
- `IN_TRANSIT`
- `DELIVERED`
- `FAILED`
- `CANCELLED`

### 5.6 Shipment lifecycle transitions
Allowed transitions:

- `PENDING -> READY_FOR_DELIVERY`
- `READY_FOR_DELIVERY -> IN_TRANSIT`
- `IN_TRANSIT -> DELIVERED`
- `IN_TRANSIT -> FAILED`
- `PENDING -> CANCELLED`
- `READY_FOR_DELIVERY -> CANCELLED`

Not allowed:

- `DELIVERED` to any other status
- `FAILED` to any other status
- `CANCELLED` to any other status
- direct transitions that skip lifecycle steps, such as `PENDING -> DELIVERED`

### 5.7 Shipment failure rule
A shipment reaching `FAILED` does **not** automatically cancel the related order.

Shipment failure and order cancellation are different business concepts.

At this stage, a failed shipment leaves the order as-is until an explicit business action is taken.

### 5.8 Shipment cancellation rule
A shipment can be cancelled only before it enters transit.

Allowed cancellation origins:

- `PENDING`
- `READY_FOR_DELIVERY`

### 5.9 Shipment state operations
Shipment state changes are operational actions and are restricted to:

- `SELLER`
- `ADMIN`

A `BUYER` cannot:

- create shipment lifecycle transitions
- mark shipments ready
- mark shipments in transit
- mark shipments delivered
- mark shipments failed
- cancel shipments operationally

A buyer may only view shipment status for owned orders.

### 5.10 Shipment API transition style
Shipment state changes must be exposed as explicit command-oriented `PATCH` endpoints.

Examples:

- ready
- in transit
- deliver
- fail
- cancel

The API must not expose a generic free-form status setter.

---

## 6. Cross-Service Lifecycle Rules

### 6.1 Product-service as catalog source of truth
`product-service` is the source of truth for:

- product existence
- active status
- current stock
- current price and name before snapshotting

### 6.2 Order-service as order source of truth
`order-service` is the source of truth for:

- order creation
- order totals
- order item snapshots
- order lifecycle

### 6.3 Shipping-service as shipment source of truth
`shipping-service` is the source of truth for:

- shipment creation
- shipment lifecycle
- shipment delivery completion
- shipment failure or cancellation

### 6.4 Delivery-driven order shipping
When a shipment reaches `DELIVERED`, `shipping-service` must call `order-service` and trigger the order transition to `SHIPPED`.

This is the official Week 2 integration rule.

### 6.5 Synchronous integration rule
At the current stage, service collaboration is intentionally synchronous and HTTP-based.

No broker, event bus or asynchronous messaging layer is introduced yet.

---

## 7. Notification Rules

### 7.1 Notification responsibility
Each service is responsible for notifying its own relevant domain events.

### 7.2 Order-service notifications
`order-service` must notify at least:

- `ORDER_CONFIRMED`
- `ORDER_CANCELLED`
- `ORDER_SHIPPED`

### 7.3 Shipping-service notifications
`shipping-service` must notify at least:

- `SHIPMENT_CREATED`
- `SHIPMENT_READY_FOR_DELIVERY`
- `SHIPMENT_IN_TRANSIT`
- `SHIPMENT_DELIVERED`
- `SHIPMENT_FAILED`
- `SHIPMENT_CANCELLED`

### 7.4 Notification-service role
`notification-service` remains a lightweight support service and does not own business rules from order or shipment domains.

It only receives and processes notification requests.

---

## 8. Role Rules

### 8.1 Roles planned for the system
The domain model already assumes these roles:

- `BUYER`
- `SELLER`
- `ADMIN`

### 8.2 Current implementation note
Formal role enforcement is planned for Week 3 through `user-service`.

At Week 2, these role rules are already accepted as official business behavior and must guide endpoint design and future authorization.

### 8.3 Buyer permissions
A buyer can:

- browse active products
- manage cart
- checkout
- view owned orders
- view owned shipment status

A buyer cannot:

- manage product lifecycle
- manage order lifecycle operationally
- manage shipment lifecycle operationally

### 8.4 Seller permissions
A seller can:

- manage owned products
- confirm or cancel valid owned orders
- create and manage shipments for owned orders
- perform valid operational transitions in product, order and shipment domains

A seller cannot:

- operate resources belonging to other sellers

### 8.5 Admin permissions
An admin can:

- operate all products
- operate all orders
- operate all shipments
- perform operational overrides when needed

---

## 9. Infrastructure-Constrained Rules

### 9.1 Shared database rule
The current infrastructure uses a single shared PostgreSQL instance.

This is an intentional MVP infrastructure decision and does not change service domain ownership.

### 9.2 Simplicity rule
The project explicitly avoids premature introduction of:

- Kafka
- distributed workflow engines
- Kubernetes
- advanced deployment automation
- complex security frameworks
- split shipment orchestration
- premature pagination or caching strategies

### 9.3 Testing priority
Testing priority remains:

1. service layer
2. business rules
3. controller layer

This priority applies to newly introduced shipment functionality as well.

---

## 10. Official Week 2 Baseline

At the end of Week 2, the official business baseline is:

- `product-service` is already integrated
- cart lives inside `order-service`
- orders are created from validated product snapshots
- shipments are created only for confirmed orders
- shipment lifecycle is independent from order lifecycle
- orders become `SHIPPED` only after shipment delivery completion
- `BUYER` does not operate lifecycle states
- lifecycle transitions are restricted to operational actors
- notifications are emitted by the service that owns the domain event
- synchronous HTTP communication remains the selected integration model