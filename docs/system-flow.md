# System Flow

## 1. Overview

This document describes the operational flow of the Order Management System after the completion of Week 2.

The system now includes:

- `product-service` as the product catalog service
- `order-service` as the transactional core
- cart management inside `order-service`
- `shipping-service` as the shipment lifecycle service
- `notification-service` as a lightweight support service

The current implemented flow covers:

- product validation through `product-service`
- cart operations and checkout inside `order-service`
- order lifecycle handling
- shipment lifecycle handling
- synchronous service-to-service HTTP communication
- notification delivery for relevant order and shipment events
- infrastructure support through Docker Compose and Jenkins

---

## 2. Product Validation Flow

### 2.1 Product lookup
When an order or cart checkout needs product information, `order-service` calls `product-service`.

`product-service` acts as the source of truth for:

- product existence
- product name
- product price
- active status
- available stock

### 2.2 Validation result
For each requested product, `order-service` validates:

- that the product exists
- that the product is active
- that enough stock is available for the requested quantity

If any validation fails, order creation or checkout is rejected.

### 2.3 Snapshot creation
After successful validation, `order-service` builds a snapshot for each purchased item containing:

- `productId`
- product name at purchase time
- product price at purchase time
- ordered quantity

This snapshot becomes part of the order and must remain stable even if product data changes later.

---

## 3. Cart Flow

### 3.1 Cart ownership
At the current stage, cart ownership is temporarily tied to `customerName`.

This is an MVP decision that remains in place until `user-service` is introduced.

### 3.2 Cart actions
The cart inside `order-service` supports:

- add item
- list cart
- update quantity
- remove item
- clear cart
- checkout

### 3.3 Checkout flow
The checkout flow is:

1. buyer prepares a cart
2. `order-service` revalidates products through `product-service`
3. `order-service` builds order item snapshots
4. `order-service` calculates totals
5. `order-service` creates an order with status `CREATED`
6. `order-service` persists the order
7. `order-service` clears the cart

### 3.4 Checkout result
A successful checkout does not confirm or ship the order.  
It only creates the order in `CREATED`.

---

## 4. Order Lifecycle Flow

### 4.1 Official order statuses
The order lifecycle uses:

- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

### 4.2 Created order
When an order is first created, it starts in `CREATED`.

At this point:

- the order exists
- item snapshots are already stored
- total amount is already calculated
- no shipment exists yet

### 4.3 From `CREATED`
Allowed transitions:

- `CONFIRMED`
- `CANCELLED`

Not allowed:

- direct `SHIPPED`

### 4.4 From `CONFIRMED`
Allowed transitions:

- `CANCELLED`
- shipment creation in `shipping-service`

Not directly allowed as a standalone business step:

- manual shipping completion disconnected from shipment delivery

### 4.5 Meaning of `SHIPPED`
`SHIPPED` is not treated as an isolated manual order transition anymore.

It is now the business result of successful shipment delivery.

### 4.6 Shipping completion rule
An order becomes `SHIPPED` only when the related shipment reaches `DELIVERED` in `shipping-service`.

### 4.7 Terminal order states
These are terminal order states:

- `CANCELLED`
- `SHIPPED`

No further order lifecycle transitions are allowed after that.

---

## 5. Shipping Flow

### 5.1 Shipping introduction
Week 2 introduces `shipping-service` as a standalone service responsible for shipment lifecycle management.

It does not replace `order-service`, but extends the system after order confirmation.

### 5.2 Shipment creation prerequisites
A shipment can be created only if:

- the related order exists
- the related order is in `CONFIRMED`
- the order does not already have a shipment

### 5.3 Official shipment statuses
The shipment lifecycle uses:

- `PENDING`
- `READY_FOR_DELIVERY`
- `IN_TRANSIT`
- `DELIVERED`
- `FAILED`
- `CANCELLED`

### 5.4 Shipment lifecycle flow
The expected shipment lifecycle is:

1. shipment is created in `PENDING`
2. shipment moves to `READY_FOR_DELIVERY`
3. shipment moves to `IN_TRANSIT`
4. shipment ends in one of:
   - `DELIVERED`
   - `FAILED`

Shipment may also be cancelled before transit:

- `PENDING -> CANCELLED`
- `READY_FOR_DELIVERY -> CANCELLED`

### 5.5 Shipment transition API style
Shipment transitions are command-based and exposed through explicit `PATCH` endpoints.

Examples:

- `PATCH /api/shipments/{id}/ready`
- `PATCH /api/shipments/{id}/in-transit`
- `PATCH /api/shipments/{id}/deliver`
- `PATCH /api/shipments/{id}/fail`
- `PATCH /api/shipments/{id}/cancel`

The API does not expose a generic free-form status setter.

### 5.6 Delivery-driven order completion
When a shipment reaches `DELIVERED`:

1. `shipping-service` persists the shipment as `DELIVERED`
2. `shipping-service` notifies `notification-service`
3. `shipping-service` calls `order-service`
4. `order-service` transitions the related order to `SHIPPED`
5. `order-service` notifies `notification-service`

### 5.7 Failed shipment behavior
If a shipment reaches `FAILED`, the order is not automatically cancelled.

Shipment failure and order cancellation are treated as different business concepts.

### 5.8 One shipment per order
At the current stage, one order has at most one shipment.

Split shipments and partial deliveries are out of scope.

---

## 6. Notification Flow

### 6.1 Notification ownership
Each service is responsible for notifying its own domain events.

### 6.2 Order-service notifications
`order-service` sends notifications for relevant order lifecycle events, including:

- `ORDER_CONFIRMED`
- `ORDER_CANCELLED`
- `ORDER_SHIPPED`

### 6.3 Shipping-service notifications
`shipping-service` sends notifications for relevant shipment lifecycle events, including:

- `SHIPMENT_CREATED`
- `SHIPMENT_READY_FOR_DELIVERY`
- `SHIPMENT_IN_TRANSIT`
- `SHIPMENT_DELIVERED`
- `SHIPMENT_FAILED`
- `SHIPMENT_CANCELLED`

### 6.4 Notification-service role
`notification-service` is a lightweight support service.

It receives notification requests and processes them, but it does not own business rules from other services.

---

## 7. Error Flow

### 7.1 Product validation errors
If a product does not exist, is inactive or has insufficient stock, order creation or checkout is rejected in `order-service`.

### 7.2 Order validation errors for shipping
If `shipping-service` tries to create a shipment for:

- a non-existing order
- an order not in `CONFIRMED`
- an order that already has a shipment

the shipment creation request is rejected.

### 7.3 Shipment transition errors
If a shipment transition violates lifecycle rules, `shipping-service` rejects the request.

Examples:

- trying to deliver from `PENDING`
- trying to cancel from `DELIVERED`
- trying to move from `FAILED` to another state

### 7.4 Cross-service integration errors
The current architecture uses synchronous HTTP calls.

Because of this, cross-service failures can produce partial completion scenarios.

Example:

- `shipping-service` persists `DELIVERED`
- notification is sent
- the call to `order-service` fails
- the shipment is `DELIVERED` but the order remains `CONFIRMED`

This limitation is currently accepted as part of the MVP simplicity strategy.

---

## 8. Persistence Flow

### 8.1 Product persistence
`product-service` persists catalog data and remains the source of truth for current product state.

### 8.2 Order persistence
`order-service` persists:

- orders
- order items
- cart
- cart items

It also stores product purchase snapshots inside the order domain.

### 8.3 Shipment persistence
`shipping-service` persists:

- shipments
- shipment status
- shipment timestamps

### 8.4 Infrastructure note
At the current stage, all services use the same PostgreSQL instance at infrastructure level.

This does not change the domain ownership responsibilities of each service.

---

## 9. Internal Coordination Inside order-service

### 9.1 Order creation coordination
Inside `order-service`, order creation is coordinated through dedicated services that separate:

- catalog validation
- product snapshot creation
- order creation
- order lifecycle handling

### 9.2 Cart coordination
Cart operations remain inside `order-service` and are coordinated without introducing a dedicated cart microservice.

### 9.3 Order shipping coordination
`order-service` is still the source of truth for order status, but its `SHIPPED` transition is now driven by shipment delivery completion coming from `shipping-service`.

---

## 10. End-to-End Flow Summary

### 10.1 Happy path
The main implemented end-to-end flow is:

1. seller creates and activates products
2. buyer browses active products
3. buyer adds products to cart
4. buyer performs checkout
5. `order-service` creates order in `CREATED`
6. seller or admin confirms the order
7. `shipping-service` creates shipment for the confirmed order
8. shipment moves from `PENDING` to `READY_FOR_DELIVERY`
9. shipment moves to `IN_TRANSIT`
10. shipment reaches `DELIVERED`
11. `shipping-service` calls `order-service`
12. `order-service` marks the order as `SHIPPED`
13. both services emit their own notifications

### 10.2 Important rule summary
The system does not treat order shipping as a direct manual completion step anymore.  
Order shipping completion is now derived from shipment delivery completion.

---

## 11. Current Week 2 Baseline

At the end of Week 2, the implemented baseline is:

- `product-service` exists and is integrated with `order-service`
- `order-service` already manages order creation, validation, totals and lifecycle
- cart is implemented inside `order-service`
- `shipping-service` exists and manages shipment lifecycle
- shipment creation is restricted to confirmed orders
- shipment transitions use explicit command-style `PATCH` endpoints
- shipment delivery triggers order shipping completion
- `notification-service` receives both order and shipment events
- Docker Compose includes the integrated services
- Jenkins validates the main services through build, test and package stages