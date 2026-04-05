package com.mateo_baccillere.shipping.controller;


import com.mateo_baccillere.shipping.dto.CreateShipmentRequest;
import com.mateo_baccillere.shipping.dto.ShipmentResponse;
import com.mateo_baccillere.shipping.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse response = shipmentService.createShipment(request);
        URI location = URI.create("/api/shipments/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getShipmentById(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ShipmentResponse> getShipmentByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(shipmentService.getShipmentByOrderId(orderId));
    }

    @PatchMapping("/{id}/ready")
    public ResponseEntity<ShipmentResponse> markReadyForDelivery(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.markReadyForDelivery(id));
    }

    @PatchMapping("/{id}/in-transit")
    public ResponseEntity<ShipmentResponse> markInTransit(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.markInTransit(id));
    }

    @PatchMapping("/{id}/deliver")
    public ResponseEntity<ShipmentResponse> markDelivered(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.markDelivered(id));
    }

    @PatchMapping("/{id}/fail")
    public ResponseEntity<ShipmentResponse> markFailed(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.markFailed(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ShipmentResponse> cancelShipment(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.cancelShipment(id));
    }
}
