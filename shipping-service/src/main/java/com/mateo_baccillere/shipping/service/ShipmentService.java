package com.mateo_baccillere.shipping.service;


import com.mateo_baccillere.shipping.client.OrderClient;
import com.mateo_baccillere.shipping.domain.Shipment;
import com.mateo_baccillere.shipping.domain.ShipmentStatus;
import com.mateo_baccillere.shipping.dto.CreateShipmentRequest;
import com.mateo_baccillere.shipping.dto.OrderResponse;
import com.mateo_baccillere.shipping.dto.ShipmentResponse;
import com.mateo_baccillere.shipping.exception.DuplicateShipmentException;
import com.mateo_baccillere.shipping.exception.InvalidOrderStateException;
import com.mateo_baccillere.shipping.exception.ShipmentNotFoundException;
import com.mateo_baccillere.shipping.mapper.ShipmentMapper;
import com.mateo_baccillere.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderClient orderClient;

    public ShipmentService(ShipmentRepository shipmentRepository, OrderClient orderClient) {
        this.shipmentRepository = shipmentRepository;
        this.orderClient = orderClient;
    }

    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        if (shipmentRepository.existsByOrderId(request.orderId())) {
            throw new DuplicateShipmentException(request.orderId());
        }

        OrderResponse order = orderClient.getOrderById(request.orderId());

        if (!"CONFIRMED".equalsIgnoreCase(order.status())) {
            throw new InvalidOrderStateException(request.orderId(), order.status());
        }

        Shipment shipment = Shipment.builder()
                .orderId(request.orderId())
                .customerName(request.customerName())
                .shippingAddress(request.shippingAddress())
                .status(ShipmentStatus.PENDING)
                .build();

        Shipment savedShipment = shipmentRepository.save(shipment);
        return ShipmentMapper.toResponse(savedShipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentById(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        return ShipmentMapper.toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipmentByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShipmentNotFoundException(
                        "Shipment not found for order id: " + orderId
                ));

        return ShipmentMapper.toResponse(shipment);
    }
}
