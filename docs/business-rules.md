# Business Rules

## Overview

This document defines the current business rules for the MVP at the end of Week 1.

The system currently includes:

- `order-service` as the core service
- `product-service` as the source of truth for catalog data
- `notification-service` as a lightweight support service
- cart functionality inside `order-service`

The rules below reflect the real implemented behavior of the current system.

## Product Rules

- product name must not be blank
- product price must be greater than zero
- product stock must not be negative
- product `active` flag must not be null
- duplicate product names are not allowed

## Product Lifecycle Rules

- a product can be created as active or inactive depending on the request
- a product can be activated
- a product can be deactivated
- inactive products cannot be used for order creation
- inactive products cannot be used for successful checkout

## Order Creation Rules

- an order must contain at least one item
- `customerName` must not be blank
- each item must include a valid `productId`
- each item quantity must be greater than zero
- the order starts with status `CREATED`

## Catalog Validation Rules for Orders

Before an order is created, each requested product must be validated through `product-service`.

Validation rules:

- the product must exist
- the product must be active
- the requested quantity must not exceed available stock

The client does not define product name or unit price as the source of truth.

Those values are obtained from `product-service`.

## Historical Snapshot Rules

When an order is created, each `OrderItem` stores:

- `productId`
- `productName`
- `unitPrice`
- `quantity`
- `subtotal`

`productName` and `unitPrice` are stored as snapshots to preserve the historical purchase data even if catalog data changes later.

## Order Calculation Rules

- the subtotal of each item is calculated as:

`quantity * unitPrice`

- the total amount of the order is calculated as the sum of all item subtotals

## Order Status Rules

Possible statuses:

- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

## Allowed Transitions

### From `CREATED`
- can move to `CONFIRMED`
- can move to `CANCELLED`

### From `CONFIRMED`
- can move to `SHIPPED`
- can move to `CANCELLED`

### From `CANCELLED`
- cannot move to any other state

### From `SHIPPED`
- cannot move to any other state

## Invalid Transition Examples

- `CREATED -> SHIPPED`
- `CANCELLED -> CONFIRMED`
- `SHIPPED -> CANCELLED`
- `SHIPPED -> CREATED`

## Cart Rules

- cart functionality exists inside `order-service`
- cart is currently associated with `customerName`
- if a customer does not have a cart, the system creates one on demand
- a cart can contain one or more items
- each cart item must include:
  - `productId`
  - `quantity`
- cart item quantity must be greater than zero
- an item can be updated in the cart
- an item can be removed from the cart
- the entire cart can be cleared
- cart does not keep historical states
- cart is considered ephemeral at this stage

## Catalog Validation Rules for Cart and Checkout

Product validation logic is reused for:

- direct order creation
- cart operations when required
- checkout

At checkout time, all items are validated again through `product-service`.

Checkout validation rules:

- every product must still exist
- every product must still be active
- every requested quantity must still be available in stock

If any validation fails, checkout must fail and the order must not be created.

## Checkout Rules

- checkout can only succeed if the cart contains valid items
- checkout creates a new order with status `CREATED`
- checkout reuses the same order construction rules as direct order creation
- after a successful checkout, the cart is cleared

## Notification Rules

A notification must be sent when:

- an order is confirmed
- an order is cancelled
- an order is shipped

## Notification Payload Rules

Each notification should include:

- `orderId`
- `eventType`
- `message`

Example event types:

- `ORDER_CONFIRMED`
- `ORDER_CANCELLED`
- `ORDER_SHIPPED`

## Error Handling Rules

The system should return a meaningful error when:

- an order does not exist
- a product does not exist
- a product is inactive
- stock is insufficient
- a cart item is invalid
- a request payload is invalid
- a status transition is not allowed

## Current Scope Limitations

The current MVP does not include:

- authentication
- authorization
- payment validation
- retry logic for notification failures
- notification persistence in a database
- advanced resilience patterns for inter-service communication
- user ownership model
- shipment management


# Shipping and order lifecycle alignment

## Order status ownership

The `order-service` remains the source of truth for the order lifecycle.  
However, once `shipping-service` is introduced, the `SHIPPED` status in `order-service` must no longer be triggered manually as a standalone business step disconnected from delivery.

## Updated order lifecycle rule

An order can move through these statuses:

- `CREATED`
- `CONFIRMED`
- `CANCELLED`
- `SHIPPED`

### Official rule

An order must transition to `SHIPPED` only when its related shipment reaches `DELIVERED` in `shipping-service`.

This means:

- confirming an order does not mark it as shipped
- creating a shipment does not mark it as shipped
- moving a shipment to `IN_TRANSIT` does not mark it as shipped
- only `DELIVERED` in `shipping-service` triggers `SHIPPED` in `order-service`

## Shipment lifecycle

The shipment lifecycle is managed by `shipping-service` and uses the following statuses:

- `PENDING`
- `READY_FOR_DELIVERY`
- `IN_TRANSIT`
- `DELIVERED`
- `FAILED`
- `CANCELLED`

## Shipment creation rules

A shipment can only be created when:

- the order exists
- the order is in `CONFIRMED`
- the order does not already have a shipment assigned

## Shipment transition rules

Shipment transitions are command-based and must not expose a generic free-form status update.

### Allowed transitions

- `PENDING -> READY_FOR_DELIVERY`
- `READY_FOR_DELIVERY -> IN_TRANSIT`
- `IN_TRANSIT -> DELIVERED`
- `IN_TRANSIT -> FAILED`
- `PENDING -> CANCELLED`
- `READY_FOR_DELIVERY -> CANCELLED`

### Not allowed

- `DELIVERED` to any other status
- `FAILED` to any other status
- `CANCELLED` to any other status
- direct transitions that skip business steps, such as `PENDING -> DELIVERED`

## Notification rules

Relevant state changes must notify `notification-service`.

### `order-service` notifications

`order-service` must notify on:

- order confirmed
- order cancelled
- order shipped

### `shipping-service` notifications

`shipping-service` must notify on:

- shipment created
- shipment ready for delivery
- shipment in transit
- shipment delivered
- shipment failed
- shipment cancelled

## Role and status permissions

Status changes are operational actions and are not available to buyers.

### `BUYER`

A buyer can:

- browse active products
- manage cart
- perform checkout
- view own orders
- view own shipment status

A buyer cannot:

- change product status
- change order status
- change shipment status

### `SELLER`

A seller can:

- manage owned products
- perform valid status transitions for owned orders
- create and manage shipments related to owned orders/products

A seller cannot:

- operate resources belonging to another seller

### `ADMIN`

An admin can:

- manage all product, order and shipment states
- perform operational overrides when needed

## Authorization principle

All status transition endpoints are reserved for `SELLER` and `ADMIN`.  
`BUYER` is restricted to commercial actions and read-only visibility over owned resources.