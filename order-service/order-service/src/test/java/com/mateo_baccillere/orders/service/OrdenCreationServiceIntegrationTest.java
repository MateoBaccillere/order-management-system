package com.mateo_baccillere.orders.service;


import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrdenCreationServiceIntegrationTest {

    @Autowired
    private OrderCreationService orderCreationService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldPersistOrderAndItemsInDatabase() {
        List<ValidatedProductSnapshot> items = List.of(
                new ValidatedProductSnapshot(1L, "Keyboard", 2, new BigDecimal("50.00"), new BigDecimal("100.00")),
                new ValidatedProductSnapshot(2L, "Mouse", 1, new BigDecimal("25.00"), new BigDecimal("25.00"))
        );

        OrderResponse response = orderCreationService.createOrderFromValidatedItems("Juan Perez", items);

        assertNotNull(response.getId());
        assertEquals("Juan Perez", response.getCustomerName());
        assertEquals("CREATED", response.getStatus());
        assertEquals(new BigDecimal("125.00"), response.getTotalAmount());
        assertEquals(2, response.getItems().size());

        Order saved = orderRepository.findById(response.getId()).orElseThrow();
        assertEquals("Juan Perez", saved.getCustomerName());
        assertEquals(OrderStatus.CREATED, saved.getStatus());
        assertEquals(new BigDecimal("125.00"), saved.getTotalAmount());
    }
}
