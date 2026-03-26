package com.mateo_baccillere.orders.exception;

public class EmptyCartCheckoutException extends RuntimeException{

    public EmptyCartCheckoutException(String customerName) {
        super("Cannot checkout an empty cart for customer: " + customerName);
    }
}
