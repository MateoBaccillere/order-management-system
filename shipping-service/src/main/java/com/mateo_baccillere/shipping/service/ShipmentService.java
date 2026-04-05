package com.mateo_baccillere.shipping.service;


import com.mateo_baccillere.shipping.client.NotificationClient;
import com.mateo_baccillere.shipping.client.OrderClient;
import com.mateo_baccillere.shipping.domain.Shipment;
import com.mateo_baccillere.shipping.domain.ShipmentStatus;
import com.mateo_baccillere.shipping.dto.CreateShipmentRequest;
import com.mateo_baccillere.shipping.dto.NotificationRequest;
import com.mateo_baccillere.shipping.dto.OrderResponse;
import com.mateo_baccillere.shipping.dto.ShipmentResponse;
import com.mateo_baccillere.shipping.exception.*;
import com.mateo_baccillere.shipping.mapper.ShipmentMapper;
import com.mateo_baccillere.shipping.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderClient orderClient;
    private final NotificationClient notificationClient;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            OrderClient orderClient,
            NotificationClient notificationClient
    ) {
        this.shipmentRepository = shipmentRepository;
        this.orderClient = orderClient;
        this.notificationClient = notificationClient;
    }

    public ShipmentResponse createShipment(CreateShipmentRequest request) {
        if (shipmentRepository.existsByOrderId(request.orderId())) {
            throw new DuplicateShipmentException(request.orderId());
        }

        OrderResponse order = orderClient.getOrderById(request.orderId());

        if (order == null) {
            throw new OrderServiceIntegrationException(
                    "Order-service returned null response for order id: " + request.orderId()
            );
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

        sendShipmentNotification(
                savedShipment,
                "SHIPMENT_CREATED",
                "Shipment " + savedShipment.getId() + " was created for order " + savedShipment.getOrderId()
        );

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

        Shipment updatedShipment = updateStatusEntity(shipment, ShipmentStatus.READY_FOR_DELIVERY);

        sendShipmentNotification(
                updatedShipment,
                "SHIPMENT_READY_FOR_DELIVERY",
                "Shipment " + updatedShipment.getId() + " is ready for delivery"
        );

        return ShipmentMapper.toResponse(updatedShipment);
    }

    public ShipmentResponse markInTransit(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.READY_FOR_DELIVERY,
                "Shipment can only move to IN_TRANSIT from READY_FOR_DELIVERY"
        );

        Shipment updatedShipment = updateStatusEntity(shipment, ShipmentStatus.IN_TRANSIT);

        sendShipmentNotification(
                updatedShipment,
                "SHIPMENT_IN_TRANSIT",
                "Shipment " + updatedShipment.getId() + " is in transit"
        );

        return ShipmentMapper.toResponse(updatedShipment);
    }

    public ShipmentResponse markDelivered(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.IN_TRANSIT,
                "Shipment can only move to DELIVERED from IN_TRANSIT"
        );

        Shipment updatedShipment = updateStatusEntity(shipment, ShipmentStatus.DELIVERED);

        sendShipmentNotification(
                updatedShipment,
                "SHIPMENT_DELIVERED",
                "Shipment " + updatedShipment.getId() + " was delivered"
        );

        orderClient.markOrderAsShipped(updatedShipment.getOrderId());

        return ShipmentMapper.toResponse(updatedShipment);
    }

    public ShipmentResponse markFailed(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.IN_TRANSIT,
                "Shipment can only move to FAILED from IN_TRANSIT"
        );

        Shipment updatedShipment = updateStatusEntity(shipment, ShipmentStatus.FAILED);

        sendShipmentNotification(
                updatedShipment,
                "SHIPMENT_FAILED",
                "Shipment " + updatedShipment.getId() + " failed during delivery"
        );

        return ShipmentMapper.toResponse(updatedShipment);
    }

    public ShipmentResponse cancelShipment(Long shipmentId) {
        Shipment shipment = findShipmentById(shipmentId);

        validateTransition(
                shipment.getStatus() == ShipmentStatus.PENDING
                        || shipment.getStatus() == ShipmentStatus.READY_FOR_DELIVERY,
                "Shipment can only be cancelled from PENDING or READY_FOR_DELIVERY"
        );

        Shipment updatedShipment = updateStatusEntity(shipment, ShipmentStatus.CANCELLED);

        sendShipmentNotification(
                updatedShipment,
                "SHIPMENT_CANCELLED",
                "Shipment " + updatedShipment.getId() + " was cancelled"
        );

        return ShipmentMapper.toResponse(updatedShipment);
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

    private Shipment updateStatusEntity(Shipment shipment, ShipmentStatus newStatus) {
        shipment.setStatus(newStatus);
        return shipmentRepository.save(shipment);
    }

    private void sendShipmentNotification(Shipment shipment, String type, String message) {
        notificationClient.sendNotification(
                new NotificationRequest(
                        shipment.getId(),
                        type,
                        message
                )
        );
    }

}
