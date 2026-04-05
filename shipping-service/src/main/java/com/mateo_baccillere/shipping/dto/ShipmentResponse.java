package com.mateo_baccillere.shipping.dto;

import com.mateo_baccillere.shipping.domain.ShipmentStatus;

import java.time.LocalDateTime;

public record ShipmentResponse(
        Long id,
        Long orderId,
        String customerName,
        String shippingAddress,
        ShipmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}