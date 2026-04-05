package com.mateo_baccillere.shipping.mapper;

import com.mateo_baccillere.shipping.domain.Shipment;
import com.mateo_baccillere.shipping.dto.ShipmentResponse;

public class ShipmentMapper {
    private ShipmentMapper() {
    }

    public static ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getCustomerName(),
                shipment.getShippingAddress(),
                shipment.getStatus(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}
