package com.mateo_baccillere.orders.service;


import com.mateo_baccillere.orders.client.NotificationClient;
import com.mateo_baccillere.orders.client.ProductClient;
import com.mateo_baccillere.orders.dto.CreateOrderItemRequest;
import com.mateo_baccillere.orders.dto.CreateOrderRequest;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.dto.ProductDetailsResponse;
import com.mateo_baccillere.orders.entity.Order;
import com.mateo_baccillere.orders.entity.OrderStatus;
import com.mateo_baccillere.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductClient productClient;

    @MockitoBean
    private NotificationClient notificationClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderEndToEndUsingRepositoryAndValidationService() {
        when(productClient.getProductById(1L)).thenReturn(buildProduct(1L, "Keyboard", "100.00", 10, true));

        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");
        request.setItems(List.of(item));

        OrderResponse response = orderService.createOrder(request);

        assertNotNull(response.getId());
        assertEquals("CREATED", response.getStatus());
        assertEquals(new BigDecimal("200.00"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());

        Order saved = orderRepository.findById(response.getId()).orElseThrow();
        assertEquals("Juan Perez", saved.getCustomerName());
        assertEquals(OrderStatus.CREATED, saved.getStatus());
        assertEquals(new BigDecimal("200.00"), saved.getTotalAmount());

        verifyNoInteractions(notificationClient);
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
