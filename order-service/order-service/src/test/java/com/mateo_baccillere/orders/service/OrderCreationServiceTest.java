package com.mateo_baccillere.orders.service;



import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderItem;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderCreationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderCreationService orderCreationService;

    @Test
    void shouldCreateOrderFromValidatedItems() {
        List<ValidatedProductSnapshot> items = List.of(
                new ValidatedProductSnapshot(1L, "Keyboard", 2, new BigDecimal("50.00"), new BigDecimal("100.00")),
                new ValidatedProductSnapshot(2L, "Mouse", 1, new BigDecimal("25.00"), new BigDecimal("25.00"))
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);

            long itemId = 1L;
            for (OrderItem item : order.getItems()) {
                item.setId(itemId++);
            }
            return order;
        });

        OrderResponse response = orderCreationService.createOrderFromValidatedItems("Juan Perez", items);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Juan Perez", response.getCustomerName());
        assertEquals("CREATED", response.getStatus());
        assertEquals(new BigDecimal("125.00"), response.getTotalAmount());
        assertEquals(2, response.getItems().size());

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());

        Order savedOrder = captor.getValue();
        assertEquals("Juan Perez", savedOrder.getCustomerName());
        assertEquals(new BigDecimal("125.00"), savedOrder.getTotalAmount());
        assertEquals(2, savedOrder.getItems().size());
        assertEquals(1L, savedOrder.getItems().get(0).getProductId());
        assertEquals("Keyboard", savedOrder.getItems().get(0).getProductName());
    }

    @Test
    void shouldMapOrderToResponse() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setCustomerName("Juan Perez");
        order.setTotalAmount(new BigDecimal("125.00"));

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProductId(1L);
        item.setProductName("Keyboard");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setSubtotal(new BigDecimal("100.00"));

        order.setItems(List.of(item));

        OrderResponse response = orderCreationService.mapToResponse(order);

        assertEquals(1L, response.getId());
        assertEquals("CREATED", response.getStatus());
        assertEquals("Juan Perez", response.getCustomerName());
        assertEquals(new BigDecimal("125.00"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals("Keyboard", response.getItems().get(0).getProductName());
    }
}
