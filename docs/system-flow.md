# System Flow

## Overview

This document describes the current functional flow of the system at the end of Week 1.

The system currently includes:

- `order-service` as the core service
- `product-service` as the source of truth for catalog data
- `notification-service` as a lightweight support service
- cart functionality implemented inside `order-service`

The main goal of the current flow is to support:

- direct order creation
- cart management
- checkout from cart to order
- order lifecycle transitions
- notification of relevant order events

---

## Current Services and Roles

### order-service
Core service responsible for:

- direct order creation
- cart operations
- checkout
- order persistence
- order lifecycle transitions
- coordination with external services

### product-service
Catalog service responsible for:

- product creation
- product retrieval
- product listing
- stock updates
- activation and deactivation
- catalog validation source of truth

### notification-service
Support service responsible for:

- receiving order event notifications
- storing notifications in memory
- exposing notifications for inspection

---

## High-Level Flow

The current system supports two main entry flows:

1. **Direct order flow**
2. **Cart checkout flow**

Both flows rely on `product-service` for product validation.

---

# 1. Direct Order Flow

## Goal

Allow a client to create an order directly in `order-service` without using the cart.

## Request Input

The client sends a request to `order-service` with:

- `customerName`
- one or more items
- each item contains:
  - `productId`
  - `quantity`

## Direct Order Steps

### Step 1 — Client sends order creation request
The client calls:

`POST /api/orders`

Example payload:

```json
{
  "customerName": "Juan Perez",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

## Step 2 — `order-service` validates request structure

`order-service` validates:

- `customerName` is not blank
- order contains at least one item
- each item has a valid `productId`
- each quantity is greater than zero

## Step 3 — `order-service` validates products through `product-service`

For each item, `order-service` calls `product-service` to obtain catalog data.

Validation includes:

- product exists
- product is active
- stock is sufficient for requested quantity

At this stage, `product-service` is the source of truth for:

- product name
- product price
- product stock
- product active status

## Step 4 — `order-service` builds internal validated snapshots

After successful validation, `order-service` creates internal validated product snapshots.

These snapshots are used to avoid coupling business flow directly to the remote DTO structure.

## Step 5 — `order-service` creates the order

`order-service` creates:

- `Order`
- `OrderItem` entries

Each `OrderItem` stores:

- `productId`
- `productName`
- `unitPrice`
- `quantity`
- `subtotal`

`productName` and `unitPrice` are stored as historical snapshots.

### Step 6 — total amount is calculated
The order total is calculated as the sum of item subtotals.

Formula:

`subtotal = quantity * unitPrice`

`totalAmount = sum of all subtotals`

## Step 7 — order is persisted

The order is stored in PostgreSQL with initial status:

- `CREATED`

## Step 8 — order lifecycle continues later

After creation, the order can continue through its normal lifecycle:

- confirm
- cancel
- ship

# 2. Cart Flow

## Goal

Allow a client to build a temporary cart and convert it into an order through checkout.

## Current Cart Design

At the current stage:

- cart is implemented inside `order-service`
- no separate cart microservice exists
- cart is associated with `customerName`
- cart is ephemeral
- cart does not keep historical states

## 2.1 Get or Create Cart

### Client action

The client requests the current cart:

`GET /api/cart/{customerName}`

### System behavior

If the customer already has a cart:

- return it

If the customer has no cart:

- create one on demand
- persist it
- return the new empty cart

## 2.2 Add Item to Cart

### Client action

The client calls:

`POST /api/cart/{customerName}/items`

Example payload:

```json
{
  "productId": 1,
  "quantity": 2
}
```

### System behavior

`order-service`:

- retrieves or creates the cart
- validates item quantity
- validates product through catalog flow when required
- adds item to cart
- persists updated cart

### Result

The cart now contains the requested item.

## 2.3 Update Cart Item Quantity

### Client action

The client calls:

`PATCH /api/cart/{customerName}/items/{productId}`

### Example payload:
```json
{
  "quantity": 3
}
```
### System behavior

`order-service`:

- retrieves the cart
- locates the item by `productId`
- validates the new quantity
- updates the item
- persists the cart

## 2.4 Remove Cart Item

### Client action

The client calls:

`DELETE /api/cart/{customerName}/items/{productId}`

### System behavior

`order-service`:

- retrieves the cart
- removes the selected item
- persists the updated cart

## 2.5 Clear Cart

### Client action

The client calls:

`DELETE /api/cart/{customerName}`

### System behavior

`order-service`:

- retrieves the cart
- removes all items
- persists the empty cart

# 3. Checkout Flow

## Goal

Convert the current cart into an order.

## Checkout Request

The client calls:

`POST /api/cart/{customerName}/checkout`

## Checkout Steps

### Step 1 — `order-service` retrieves the cart

`order-service` loads the cart associated with `customerName`.

### Step 2 — `order-service` validates cart content

Validation includes:

- cart exists
- cart contains items
- all quantities are valid

### Step 3 — `order-service` revalidates every product through `product-service`

Before creating the order, all cart items are revalidated against `product-service`.

Validation includes:

- product still exists
- product is still active
- stock is still sufficient

This revalidation is required because catalog data may have changed after the item was added to the cart.

### Step 4 — `order-service` creates the order from validated data

`order-service` reuses the same order creation logic used for direct order creation.

This means checkout also produces:

- an `Order`
- `OrderItem` entries with product snapshots
- calculated `totalAmount`
- initial status `CREATED`

### Step 5 — order is persisted

The new order is stored in PostgreSQL.

### Step 6 — cart is cleared

After successful checkout:

- the cart is emptied
- the cart does not preserve historical checkout states

### Step 7 — normal order lifecycle continues

The newly created order can later be:

- confirmed
- cancelled
- shipped

# 4. Order Lifecycle Flow

## Goal

Allow the created order to progress through business states.

## Initial Status

Every new order starts as:

- `CREATED`

## Allowed Transitions

### From `CREATED`

Allowed:

- `CONFIRMED`
- `CANCELLED`

Not allowed:

- `SHIPPED`

### From `CONFIRMED`

Allowed:

- `SHIPPED`
- `CANCELLED`

### From `CANCELLED`

No further transitions allowed.

### From `SHIPPED`

No further transitions allowed.

## 4.1 Confirm Order

### Client action

The client calls:

`PATCH /api/orders/{id}/confirm`

### System behavior

`order-service`:

- loads the order
- validates transition from `CREATED` to `CONFIRMED`
- updates status
- persists changes
- sends notification

## 4.2 Cancel Order

### Client action

The client calls:

`PATCH /api/orders/{id}/cancel`

### System behavior

`order-service`:

- loads the order
- validates allowed cancellation transition
- updates status to `CANCELLED`
- persists changes
- sends notification

## 4.3 Ship Order

### Client action

The client calls:

`PATCH /api/orders/{id}/ship`

### System behavior

`order-service`:

- loads the order
- validates transition from `CONFIRMED` to `SHIPPED`
- updates status
- persists changes
- sends notification

# Shipping flow

## Current MVP flow after Week 1

The system already supports:

- product management in `product-service`
- cart operations inside `order-service`
- checkout creating an order in `CREATED`
- order lifecycle managed by `order-service`
- notifications through `notification-service`

## Week 2 flow extension with `shipping-service`

The next functional flow is defined as follows:

1. buyer selects products and manages cart
2. buyer performs checkout
3. `order-service` creates the order with status `CREATED`
4. seller or admin confirms the order
5. `order-service` changes order status to `CONFIRMED`
6. `shipping-service` may create a shipment only for a `CONFIRMED` order
7. shipment starts in `PENDING`
8. seller or admin advances shipment through:
   - `READY_FOR_DELIVERY`
   - `IN_TRANSIT`
   - `DELIVERED`
9. when shipment reaches `DELIVERED`, `shipping-service` calls `order-service`
10. `order-service` changes the related order status to `SHIPPED`
11. both services notify `notification-service` for their respective domain events

## Important lifecycle rule

The order status `SHIPPED` is no longer treated as an isolated manual step in `order-service`.  
It is now a derived business result triggered by shipment delivery confirmation.

## Failure and cancellation handling

If a shipment reaches `FAILED`, the order is not automatically cancelled.  
Shipment failure and order cancellation are treated as different business concepts.

If shipment is cancelled before delivery, the shipment lifecycle ends in `CANCELLED`, while order handling remains subject to explicit business action in `order-service`.

## API transition style

Shipment transitions are command-based through explicit `PATCH` endpoints.  
The API does not expose a generic `"set shipment status"` operation.

### Examples

- `PATCH /api/shipments/{id}/ready`
- `PATCH /api/shipments/{id}/in-transit`
- `PATCH /api/shipments/{id}/deliver`
- `PATCH /api/shipments/{id}/fail`
- `PATCH /api/shipments/{id}/cancel`
# 5. Notification Flow

## Goal

Track important order events through a simple support service.

## Triggered Events

At the current stage, notifications are sent when an order is:

- confirmed
- cancelled
- shipped

## Flow

### Step 1 — `order-service` changes order status

A lifecycle action succeeds in `order-service`.

### Step 2 — `order-service` builds notification payload

The payload includes information such as:

- `orderId`
- `eventType`
- `message`

### Step 3 — `order-service` calls `notification-service`

`order-service` sends an HTTP request to `notification-service`.

### Step 4 — `notification-service` stores notification

`notification-service` stores the notification in memory.

### Step 5 — notifications can be inspected

The client can query notifications through:

`GET /api/notifications`

# 6. Error Flow

## Goal

Return meaningful errors when business rules fail.

## Common Error Scenarios

### Product-related errors

- product does not exist
- product is inactive
- stock is insufficient

### Order-related errors

- order does not exist
- order has no items
- invalid status transition
- invalid request payload

### Cart-related errors

- cart item not found
- invalid quantity
- checkout attempted on empty cart

## Error Handling Approach

The system uses:

- custom exceptions
- centralized exception handling
- consistent HTTP error responses

# 7. Persistence Flow

## PostgreSQL Persistence

`order-service` persists:

- orders
- order items
- carts
- cart items

`product-service` persists:

- products

`notification-service` persists:

- notifications are not stored in database yet
- current storage is in memory only

# 8. Internal Coordination Inside `order-service`

To keep responsibilities clearer, `order-service` currently uses internal separation of concerns:

## `CatalogProductValidationService`

Centralizes product validation against `product-service`.

Used by:

- direct order creation
- cart-related validation
- checkout

## `OrderCreationService`

Centralizes order construction and persistence from validated product snapshots.

Used by:

- direct order creation
- checkout

## `OrderService`

Coordinates:

- direct order use cases
- order retrieval
- order lifecycle transitions

## `CartService`

Coordinates:

- get cart
- add item
- update quantity
- remove item
- clear cart
- checkout

# 9. End-to-End Flow Summary

## Flow A — Direct order

- Create product in `product-service`
- Send order creation request to `order-service`
- Validate product through `product-service`
- Persist order in `CREATED`
- Continue order lifecycle

## Flow B — Cart checkout

- Create product in `product-service`
- Add item to cart in `order-service`
- Update or remove items if needed
- Checkout cart
- Revalidate products through `product-service`
- Persist order in `CREATED`
- Clear cart
- Continue order lifecycle

## Flow C — Notification after lifecycle action

- Confirm, cancel, or ship an order
- `order-service` updates order status
- `order-service` sends notification
- `notification-service` stores notification in memory

# 10. Current Week 1 Baseline

At the end of Week 1, the integrated baseline of the project is:

- `order-service` implemented and acting as the core
- `product-service` implemented and integrated
- cart implemented inside `order-service`
- checkout creating orders in `CREATED`
- `notification-service` connected for order events
- PostgreSQL persistence working
- Docker Compose working for local integrated execution
- Jenkins pipeline validating the current core services

This is the functional baseline that prepares the project for the next planned growth step:

- `shipping-service` in Week 2

