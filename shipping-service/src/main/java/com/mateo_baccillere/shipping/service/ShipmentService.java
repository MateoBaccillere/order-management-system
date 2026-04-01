package com.mateo_baccillere.shipping.service;


import com.mateo_baccillere.shipping.client.OrderClient;
import com.mateo_baccillere.shipping.domain.Shipment;
import com.mateo_baccillere.shipping.domain.ShipmentStatus;
import com.mateo_baccillere.shipping.dto.CreateShipmentRequest;
import com.mateo_baccillere.shipping.dto.OrderResponse;
import com.mateo_baccillere.shipping.dto.ShipmentResponse;
import com.mateo_baccillere.shipping.exception.DuplicateShipmentException;
import com.mateo_baccillere.shipping.exception.InvalidOrderStateException;
import com.mateo_baccillere.shipping.exception.InvalidShipmentStateTransitionException;
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

        if (order == null) {
            throw new IllegalStateException("Order-service returned null response for order id: " + request.orderId());
        }

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
        Shipment shipment = findShipmentById(shipmentId);
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

    public ShipmentResponse markReadyForDelivery(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.PENDING,
                "Shipment can only move to READY_FOR_DELIVERY from PENDING"
        );

        return updateStatus(shipment, ShipmentStatus.READY_FOR_DELIVERY);
    }

    public ShipmentResponse markInTransit(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.READY_FOR_DELIVERY,
                "Shipment can only move to IN_TRANSIT from READY_FOR_DELIVERY"
        );

        return updateStatus(shipment, ShipmentStatus.IN_TRANSIT);
    }

    public ShipmentResponse markDelivered(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.IN_TRANSIT,
                "Shipment can only move to DELIVERED from IN_TRANSIT"
        );

        ShipmentResponse response = updateStatus(shipment, ShipmentStatus.DELIVERED);

        orderClient.markOrderAsShipped(shipment.getOrderId());

        return response;
    }

    public ShipmentResponse markFailed(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.IN_TRANSIT,
                "Shipment can only move to FAILED from IN_TRANSIT"
        );

        return updateStatus(shipment, ShipmentStatus.FAILED);
    }

    public ShipmentResponse cancelShipment(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.PENDING
                        || shipment.getStatus() == ShipmentStatus.READY_FOR_DELIVERY,
                "Shipment can only be cancelled from PENDING or READY_FOR_DELIVERY"
        );

        return updateStatus(shipment, ShipmentStatus.CANCELLED);
    }

    private Shipment findShipmentById(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));
    }

    private void validateTransition(boolean valid, String message) {
        if (!valid) {
            throw new InvalidShipmentStateTransitionException(message);
        }
    }

    private ShipmentResponse updateStatus(Shipment shipment, ShipmentStatus newStatus) {
        shipment.setStatus(newStatus);
        Shipment updatedShipment = shipmentRepository.save(shipment);
        return ShipmentMapper.toResponse(updatedShipment);
    }

}
