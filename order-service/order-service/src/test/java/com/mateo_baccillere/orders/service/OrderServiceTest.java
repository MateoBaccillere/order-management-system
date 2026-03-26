package com.mateo_baccillere.orders.service;


import com.mateo_baccillere.orders.client.NotificationClient;
import com.mateo_baccillere.orders.dto.CreateOrderItemRequest;
import com.mateo_baccillere.orders.dto.CreateOrderRequest;
import com.mateo_baccillere.orders.dto.NotificationRequest;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.entity.Order;
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

    @Mock
    private CatalogProductValidationService catalogProductValidationService;

    @Mock
    private OrderCreationService orderCreationService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderUsingValidatedSnapshots() {
        CreateOrderItemRequest item1 = new CreateOrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(2);

        CreateOrderItemRequest item2 = new CreateOrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");
        request.setItems(List.of(item1, item2));

        ValidatedProductSnapshot snapshot1 = new ValidatedProductSnapshot(
                1L, "Keyboard", 2, new BigDecimal("50.00"), new BigDecimal("100.00")
        );
        ValidatedProductSnapshot snapshot2 = new ValidatedProductSnapshot(
                2L, "Mouse", 1, new BigDecimal("25.00"), new BigDecimal("25.00")
        );

        OrderResponse expectedResponse = new OrderResponse(
                1L,
                "Juan Perez",
                "CREATED",
                new BigDecimal("125.00"),
                null,
                null,
                List.of()
        );

        when(catalogProductValidationService.validateAndSnapshot(1L, 2)).thenReturn(snapshot1);
        when(catalogProductValidationService.validateAndSnapshot(2L, 1)).thenReturn(snapshot2);
        when(orderCreationService.createOrderFromValidatedItems("Juan Perez", List.of(snapshot1, snapshot2)))
                .thenReturn(expectedResponse);

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals("Juan Perez", response.getCustomerName());
        assertEquals("CREATED", response.getStatus());
        assertEquals(new BigDecimal("125.00"), response.getTotalAmount());

        verify(catalogProductValidationService).validateAndSnapshot(1L, 2);
        verify(catalogProductValidationService).validateAndSnapshot(2L, 1);
        verify(orderCreationService).createOrderFromValidatedItems("Juan Perez", List.of(snapshot1, snapshot2));
        verifyNoInteractions(notificationClient);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void shouldGetOrderById() {
        Order order = buildOrder(1L, OrderStatus.CREATED);

        OrderResponse mappedResponse = new OrderResponse(
                1L,
                "Juan Perez",
                "CREATED",
                new BigDecimal("125.00"),
                null,
                null,
                List.of()
        );

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderCreationService.mapToResponse(order)).thenReturn(mappedResponse);

        OrderResponse response = orderService.getOrderById(1L);

        assertEquals(1L, response.getId());
        assertEquals("CREATED", response.getStatus());

        verify(orderRepository).findById(1L);
        verify(orderCreationService).mapToResponse(order);
    }

    @Test
    void shouldGetAllOrders() {
        Order order1 = buildOrder(1L, OrderStatus.CREATED);
        Order order2 = buildOrder(2L, OrderStatus.CONFIRMED);

        OrderResponse response1 = new OrderResponse(
                1L, "Juan Perez", "CREATED", new BigDecimal("125.00"), null, null, List.of()
        );
        OrderResponse response2 = new OrderResponse(
                2L, "Ana Lopez", "CONFIRMED", new BigDecimal("80.00"), null, null, List.of()
        );

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));
        when(orderCreationService.mapToResponse(order1)).thenReturn(response1);
        when(orderCreationService.mapToResponse(order2)).thenReturn(response2);

        List<OrderResponse> responses = orderService.getAllOrders();

        assertEquals(2, responses.size());
        assertEquals("CREATED", responses.get(0).getStatus());
        assertEquals("CONFIRMED", responses.get(1).getStatus());
    }

    @Test
    void shouldConfirmOrderWhenStatusIsCreated() {
        Order order = buildOrder(1L, OrderStatus.CREATED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderCreationService.mapToResponse(order)).thenReturn(
                new OrderResponse(1L, "Juan Perez", "CONFIRMED", new BigDecimal("125.00"), null, null, List.of())
        );

        OrderResponse response = orderService.confirmOrder(1L);

        assertEquals("CONFIRMED", response.getStatus());

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).sendNotification(captor.capture());

        NotificationRequest notification = captor.getValue();
        assertEquals(1L, notification.getOrderId());
        assertEquals("ORDER_CONFIRMED", notification.getEventType());
    }

    @Test
    void shouldThrowExceptionWhenConfirmingAlreadyConfirmedOrder() {
        Order order = buildOrder(1L, OrderStatus.CONFIRMED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        InvalidOrderStateTransitionException ex = assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> orderService.confirmOrder(1L)
        );

        assertEquals("Order is already confirmed", ex.getMessage());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldCancelOrderWhenStatusIsCreated() {
        Order order = buildOrder(1L, OrderStatus.CREATED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderCreationService.mapToResponse(order)).thenReturn(
                new OrderResponse(1L, "Juan Perez", "CANCELLED", new BigDecimal("125.00"), null, null, List.of())
        );

        OrderResponse response = orderService.cancelOrder(1L);

        assertEquals("CANCELLED", response.getStatus());

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).sendNotification(captor.capture());

        NotificationRequest notification = captor.getValue();
        assertEquals("ORDER_CANCELLED", notification.getEventType());
    }

    @Test
    void shouldCancelOrderWhenStatusIsConfirmed() {
        Order order = buildOrder(1L, OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderCreationService.mapToResponse(order)).thenReturn(
                new OrderResponse(1L, "Juan Perez", "CANCELLED", new BigDecimal("125.00"), null, null, List.of())
        );

        OrderResponse response = orderService.cancelOrder(1L);

        assertEquals("CANCELLED", response.getStatus());
        verify(notificationClient).sendNotification(any(NotificationRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenCancellingShippedOrder() {
        Order order = buildOrder(1L, OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        InvalidOrderStateTransitionException ex = assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> orderService.cancelOrder(1L)
        );

        assertEquals("Only orders in CREATED or CONFIRMED status can be cancelled", ex.getMessage());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldShipOrderWhenStatusIsConfirmed() {
        Order order = buildOrder(1L, OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderCreationService.mapToResponse(order)).thenReturn(
                new OrderResponse(1L, "Juan Perez", "SHIPPED", new BigDecimal("125.00"), null, null, List.of())
        );

        OrderResponse response = orderService.shipOrder(1L);

        assertEquals("SHIPPED", response.getStatus());

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationClient).sendNotification(captor.capture());

        NotificationRequest notification = captor.getValue();
        assertEquals("ORDER_SHIPPED", notification.getEventType());
    }

    @Test
    void shouldThrowExceptionWhenShippingCreatedOrder() {
        Order order = buildOrder(1L, OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        InvalidOrderStateTransitionException ex = assertThrows(
                InvalidOrderStateTransitionException.class,
                () -> orderService.shipOrder(1L)
        );

        assertEquals("Only orders in CONFIRMED status can be shipped", ex.getMessage());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldDeleteOrderById() {
        Order order = buildOrder(1L, OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.deleteOrderById(1L);

        verify(orderRepository).delete(order);
    }

    @Test
    void shouldThrowExceptionWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.confirmOrder(99L));

        verify(orderRepository, never()).save(any());
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
