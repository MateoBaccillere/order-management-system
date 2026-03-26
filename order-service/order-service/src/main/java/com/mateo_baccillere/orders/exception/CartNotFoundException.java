package com.mateo_baccillere.orders.exception;

public class CartNotFoundException extends RuntimeException{

    public CartNotFoundException(String customerName) {
        super("Active cart not found for customer: " + customerName);
    }
}
