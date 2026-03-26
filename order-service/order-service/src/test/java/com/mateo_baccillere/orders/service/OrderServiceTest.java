package com.mateo_baccillere.orders.service;


import com.mateo_baccillere.orders.client.NotificationClient;
import com.mateo_baccillere.orders.client.ProductClient;
import com.mateo_baccillere.orders.dto.CreateOrderItemRequest;
import com.mateo_baccillere.orders.dto.CreateOrderRequest;
import com.mateo_baccillere.orders.dto.NotificationRequest;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.dto.ProductDetailsResponse;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderItem;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.exception.InvalidOrderStateTransitionException;
import com.mateo_baccillere.orders.exception.OrderNotFoundException;
import com.mateo_baccillere.orders.exception.ProductUnavailableException;
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
    private ProductClient productClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCreateOrderAndCalculateTotalAmountUsingCatalogData() {
        CreateOrderItemRequest item1 = new CreateOrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(2);

        CreateOrderItemRequest item2 = new CreateOrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");
        request.setItems(List.of(item1, item2));

        when(productClient.getProductById(1L)).thenReturn(
                buildProduct(1L, "Keyboard", "50.00", 10, true)
        );
        when(productClient.getProductById(2L)).thenReturn(
                buildProduct(2L, "Mouse", "25.00", 5, true)
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

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals("Juan Perez", response.getCustomerName());
        assertEquals("CREATED", response.getStatus());
        assertEquals(new BigDecimal("125.00"), response.getTotalAmount());
        assertEquals(2, response.getItems().size());

        assertEquals("Keyboard", response.getItems().get(0).getProductName());
        assertEquals(new BigDecimal("50.00"), response.getItems().get(0).getUnitPrice());
        assertEquals(new BigDecimal("100.00"), response.getItems().get(0).getSubtotal());

        assertEquals("Mouse", response.getItems().get(1).getProductName());
        assertEquals(new BigDecimal("25.00"), response.getItems().get(1).getUnitPrice());
        assertEquals(new BigDecimal("25.00"), response.getItems().get(1).getSubtotal());

        verify(productClient).getProductById(1L);
        verify(productClient).getProductById(2L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldThrowExceptionWhenProductDoesNotExist() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId(99L);
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");
        request.setItems(List.of(item));

        when(productClient.getProductById(99L)).thenReturn(null);

        ProductUnavailableException ex = assertThrows(
                ProductUnavailableException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals("Product not found with id: 99", ex.getMessage());

        verify(productClient).getProductById(99L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldThrowExceptionWhenProductIsInactive() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");
        request.setItems(List.of(item));

        when(productClient.getProductById(1L)).thenReturn(
                buildProduct(1L, "Keyboard", "50.00", 10, false)
        );

        ProductUnavailableException ex = assertThrows(
                ProductUnavailableException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals("Product is inactive with id: 1", ex.getMessage());

        verify(productClient).getProductById(1L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(notificationClient);
    }

    @Test
    void shouldThrowExceptionWhenStockIsInsufficient() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(20);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");
        request.setItems(List.of(item));

        when(productClient.getProductById(1L)).thenReturn(
                buildProduct(1L, "Keyboard", "50.00", 5, true)
        );

        ProductUnavailableException ex = assertThrows(
                ProductUnavailableException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals("Insufficient stock for product id: 1", ex.getMessage());

        verify(productClient).getProductById(1L);
        verify(orderRepository, never()).save(any(Order.class));
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

    private ProductDetailsResponse buildProduct(
            Long id,
            String name,
            String price,
            Integer stock,
            Boolean active
    ) {
        ProductDetailsResponse product = new ProductDetailsResponse();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setActive(active);
        return product;
    }

}
