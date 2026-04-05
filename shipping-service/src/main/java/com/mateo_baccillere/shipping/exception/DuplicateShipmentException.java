package com.mateo_baccillere.shipping.exception;


public class DuplicateShipmentException extends RuntimeException{

    public DuplicateShipmentException(Long orderId) {
        super("Shipment already exists for order id: " + orderId);
    }
}
