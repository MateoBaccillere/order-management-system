package com.mateo_baccillere.shipping.exception;

public class InvalidOrderStateException extends RuntimeException{

    public InvalidOrderStateException(Long orderId, String status) {
        super("Order with id " + orderId + " is not eligible for shipment creation. Current status: " + status);
    }
}
