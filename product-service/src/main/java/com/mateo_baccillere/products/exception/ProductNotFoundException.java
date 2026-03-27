package com.mateo_baccillere.products.exception;

public class ProductNotFoundException extends RuntimeException{

    public ProductNotFoundException(Long productId) {
        super("Product not found with id: " + productId);
    }
}
