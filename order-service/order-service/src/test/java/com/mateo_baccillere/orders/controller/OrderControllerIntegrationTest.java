package com.mateo_baccillere.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mateo_baccillere.orders.dto.CreateOrderItemRequest;
import com.mateo_baccillere.orders.dto.CreateOrderRequest;
import com.mateo_baccillere.orders.dto.OrderResponse;
import com.mateo_baccillere.orders.exception.GlobalExceptionHandler;
import com.mateo_baccillere.orders.exception.ProductUnavailableException;
import com.mateo_baccillere.orders.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
public class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrderService orderService;

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");

        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        OrderResponse response = new OrderResponse(
                1L,
                "Juan Perez",
                "CREATED",
                new BigDecimal("100.00"),
                null,
                null,
                List.of()
        );

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Juan Perez"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(100.00));
    }

    @Test
    void shouldReturnBadRequestWhenProductIsUnavailable() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerName("Juan Perez");

        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId(99L);
        item.setQuantity(1);
        request.setItems(List.of(item));

        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new ProductUnavailableException("Product not found with id: 99"));

        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));
    }

    @Test
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        String invalidJson = """
                {
                  "customerName": "Juan Perez",
                  "items": [
                    {
                      "productId": 1,
                      "quantity": 0
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
