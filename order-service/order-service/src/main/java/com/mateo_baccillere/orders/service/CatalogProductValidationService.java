package com.mateo_baccillere.orders.service;


import com.mateo_baccillere.orders.client.ProductClient;
import com.mateo_baccillere.orders.dto.ProductDetailsResponse;
import com.mateo_baccillere.orders.exception.ProductUnavailableException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CatalogProductValidationService {
    private final ProductClient productClient;

    public CatalogProductValidationService(ProductClient productClient) {
        this.productClient = productClient;
    }

    public ValidatedProductSnapshot validateAndSnapshot(Long productId, Integer quantity) {
        ProductDetailsResponse product = productClient.getProductById(productId);

        if (product == null) {
            throw new ProductUnavailableException("Product not found with id: " + productId);
        }

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new ProductUnavailableException("Product is inactive with id: " + productId);
        }

        if (product.getStock() == null || quantity > product.getStock()) {
            throw new ProductUnavailableException("Insufficient stock for product id: " + productId);
        }

        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        return new ValidatedProductSnapshot(
                product.getId(),
                product.getName(),
                quantity,
                product.getPrice(),
                subtotal
        );
    }
}
