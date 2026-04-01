package com.mateo_baccillere.shipping.controller;


import com.mateo_baccillere.shipping.dto.CreateShipmentRequest;
import com.mateo_baccillere.shipping.dto.ShipmentResponse;
import com.mateo_baccillere.shipping.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        return shipmentService.createShipment(request);
    }

    @GetMapping("/{id}")
    public ShipmentResponse getShipmentById(@PathVariable Long id) {
        return shipmentService.getShipmentById(id);
    }

    @GetMapping("/order/{orderId}")
    public ShipmentResponse getShipmentByOrderId(@PathVariable Long orderId) {
        return shipmentService.getShipmentByOrderId(orderId);
    }
}
