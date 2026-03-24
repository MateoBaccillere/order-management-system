package com.mateo_baccillere.orders.exception;

public class InvalidOrderStateTransitionException extends RuntimeException {


    public InvalidOrderStateTransitionException(String message) {
        super(message);
    }
}
