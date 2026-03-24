package com.mateo_baccillere.orders.service;


import com.mateo_baccillere.orders.client.NotificationClient;
import com.mateo_baccillere.orders.dto.CreateOrderItemRequest;
import com.mateo_baccillere.orders.dto.CreateOrderRequest;
import com.mateo_baccillere.orders.dto.NotificationRequest;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderItem;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.exception.InvalidOrderStateTransitionException;
import com.mateo_baccillere.orders.exception.OrderNotFoundException;
import com.mateo_baccillere.orders.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderAndCalculateTotalAmount() {
        CreateOrderItemRequest item1 = new CreateOrderItemRequest();
        item1.setProductName("Keyboard");
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("50.00"));

        CreateOrderItemRequest item2 = new CreateOrderItemRequest();
        item2.setProductName("Mouse");
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("25.00"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");
        request.setItems(List.of(item1, item2));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);

            long itemId = 1L;
            for (OrderItem item : order.getItems()) {
                item.setId(itemId++);
            }

            return order;
        });

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals("Juan Perez", response.getCustomerName());
        assertEquals("CREATED", response.getStatus());
        assertEquals(new BigDecimal("125.00"), response.getTotalAmount());
        assertEquals(2, response.getItems().size());

        verify(orderRepository, times(1)).save(any(Order.class));
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldConfirmOrderWhenStatusIsCreated() {
        Order order = buildOrder(1L, OrderStatus.CREATED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.confirmOrder(1L);

        assertEquals("CONFIRMED", response.getStatus());

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).sendNotification(captor.capture());

        NotificationRequest notification = captor.getValue();
        assertEquals(1L, notification.getOrderId());
        assertEquals("ORDER_CONFIRMED", notification.getEventType());
    }

    @Test
    void shouldCancelOrderWhenStatusIsCreated() {
        Order order = buildOrder(1L, OrderStatus.CREATED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(1L);

        assertEquals("CANCELLED", response.getStatus());

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).sendNotification(captor.capture());

        NotificationRequest notification = captor.getValue();
        assertEquals(1L, notification.getOrderId());
        assertEquals("ORDER_CANCELLED", notification.getEventType());
    }

    @Test
    void shouldShipOrderWhenStatusIsConfirmed() {
        Order order = buildOrder(1L, OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.shipOrder(1L);

        assertEquals("SHIPPED", response.getStatus());

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).sendNotification(captor.capture());

        NotificationRequest notification = captor.getValue();
        assertEquals(1L, notification.getOrderId());
        assertEquals("ORDER_SHIPPED", notification.getEventType());
    }

    @Test
    void shouldThrowExceptionWhenShippingCreatedOrder() {
        Order order = buildOrder(1L, OrderStatus.CREATED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateTransitionException.class, () -> orderService.shipOrder(1L));

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.confirmOrder(99L));

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(notificationClient);
    }

    private Order buildOrder(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setCustomerName("Juan Perez");
        order.setStatus(status);
        order.setTotalAmount(new BigDecimal("125.00"));
        order.setItems(List.of());

        return order;
    }

}
