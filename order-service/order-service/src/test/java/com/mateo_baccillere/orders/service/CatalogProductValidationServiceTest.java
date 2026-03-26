package com.mateo_baccillere.orders.service;

import com.mateo_baccillere.orders.client.ProductClient;
import com.mateo_baccillere.orders.dto.ProductDetailsResponse;
import com.mateo_baccillere.orders.exception.ProductUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CatalogProductValidationServiceTest {


    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CatalogProductValidationService catalogProductValidationService;

    @Test
    void shouldValidateProductAndBuildSnapshot() {
        when(productClient.getProductById(1L)).thenReturn(buildProduct(1L, "Keyboard", "100.00", 10, true));

        ValidatedProductSnapshot snapshot = catalogProductValidationService.validateAndSnapshot(1L, 2);

        assertEquals(1L, snapshot.getProductId());
        assertEquals("Keyboard", snapshot.getProductName());
        assertEquals(2, snapshot.getQuantity());
        assertEquals(new BigDecimal("100.00"), snapshot.getUnitPrice());
        assertEquals(new BigDecimal("200.00"), snapshot.getSubtotal());
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {
        when(productClient.getProductById(99L)).thenReturn(null);

        ProductUnavailableException ex = assertThrows(
                ProductUnavailableException.class,
                () -> catalogProductValidationService.validateAndSnapshot(99L, 1)
        );

        assertEquals("Product not found with id: 99", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenProductIsInactive() {
        when(productClient.getProductById(1L)).thenReturn(buildProduct(1L, "Keyboard", "100.00", 10, false));

        ProductUnavailableException ex = assertThrows(
                ProductUnavailableException.class,
                () -> catalogProductValidationService.validateAndSnapshot(1L, 1)
        );

        assertEquals("Product is inactive with id: 1", ex.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenStockIsInsufficient() {
        when(productClient.getProductById(1L)).thenReturn(buildProduct(1L, "Keyboard", "100.00", 2, true));

        ProductUnavailableException ex = assertThrows(
                ProductUnavailableException.class,
                () -> catalogProductValidationService.validateAndSnapshot(1L, 5)
        );

        assertEquals("Insufficient stock for product id: 1", ex.getMessage());
    }

    private ProductDetailsResponse buildProduct(
            Long id,
            String name,
            String price,
            Integer stock,
            Boolean active
    ) {
        ProductDetailsResponse product = new ProductDetailsResponse();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setActive(active);
        return product;
    }

}
