package com.mateo_baccillere.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateShipmentRequest(
        @NotNull(message = "Order id is required")
        Long orderId,

        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Shipping address is required")
        String shippingAddress
) {
}