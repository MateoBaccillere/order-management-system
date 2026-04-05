package com.mateo_baccillere.shipping.exception;

public class InvalidShipmentStateTransitionException extends RuntimeException{

    public InvalidShipmentStateTransitionException(String message) {
        super(message);
    }
}
