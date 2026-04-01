package com.mateo_baccillere.shipping.dto;

public record OrderResponse(

        Long id,
        String customerName,
        String status
) {
}
