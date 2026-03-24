# Business Rules

## Order Creation Rules

- An order must contain at least one item
- `customerName` must not be blank
- Each item must have a valid `productName`
- Each item quantity must be greater than zero
- Each item unit price must be greater than zero
- The order starts with status `CREATED`

## Order Calculation Rules

- The subtotal of each item is calculated as:

`quantity * unitPrice`

- The total amount of the order is calculated as the sum of all item subtotals

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
- a request payload is invalid
- a status transition is not allowed

## Current Scope Limitations

The current MVP does not include:

- authentication
- authorization
- stock validation
- payment validation
- retry logic for notification failures
- notification persistence in a database