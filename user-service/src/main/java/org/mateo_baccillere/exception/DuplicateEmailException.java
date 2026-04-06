package org.mateo_baccillere.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
