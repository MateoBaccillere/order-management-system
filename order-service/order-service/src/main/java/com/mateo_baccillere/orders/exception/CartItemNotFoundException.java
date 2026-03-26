package com.mateo_baccillere.orders.exception;

public class CartItemNotFoundException extends RuntimeException{

    public CartItemNotFoundException(Long productId) {
        super("Cart item not found for product id: " + productId);
    }
}
