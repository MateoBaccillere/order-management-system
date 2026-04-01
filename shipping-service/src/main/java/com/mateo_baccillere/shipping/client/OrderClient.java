package com.mateo_baccillere.shipping.client;

import com.mateo_baccillere.shipping.dto.OrderResponse;
import com.mateo_baccillere.shipping.exception.OrderNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderClient {

    private final RestTemplate restTemplate;
    private final String orderServiceBaseUrl;

    public OrderClient(RestTemplate restTemplate,
                       @Value("${clients.order-service.base-url}") String orderServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceBaseUrl = orderServiceBaseUrl;
    }

    public OrderResponse getOrderById(Long orderId) {
        String url = orderServiceBaseUrl + "/api/orders/" + orderId;

        try {
            ResponseEntity<OrderResponse> response =
                    restTemplate.getForEntity(url, OrderResponse.class);

            return response.getBody();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new OrderNotFoundException(orderId);
        }
    }
}
