package com.mateo_baccillere.shipping.client;

import com.mateo_baccillere.shipping.dto.OrderResponse;
import com.mateo_baccillere.shipping.exception.OrderNotFoundException;
import com.mateo_baccillere.shipping.exception.OrderServiceIntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class OrderClient {

    private final RestTemplate restTemplate;
    private final String orderServiceBaseUrl;

    public OrderClient(
            RestTemplate restTemplate,
            @Value("${clients.order-service.base-url}") String orderServiceBaseUrl
    ) {
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
        } catch (HttpStatusCodeException ex) {
            throw new OrderServiceIntegrationException(
                    "Order-service getOrderById failed. status=" + ex.getStatusCode()
                            + ", body=" + ex.getResponseBodyAsString()
            );
        } catch (ResourceAccessException ex) {
            throw new OrderServiceIntegrationException(
                    "Order-service is unreachable at " + url + ". cause=" + ex.getMessage()
            );
        } catch (RestClientException ex) {
            throw new OrderServiceIntegrationException(
                    "Failed to retrieve order from order-service. cause=" + ex.getMessage()
            );
        }
    }

    public void markOrderAsShipped(Long orderId) {
        String url = orderServiceBaseUrl + "/api/orders/" + orderId + "/ship";

        try {
            RequestEntity<Void> request = new RequestEntity<>(HttpMethod.PATCH, URI.create(url));
            restTemplate.exchange(request, Void.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new OrderNotFoundException(orderId);
        } catch (HttpStatusCodeException ex) {
            throw new OrderServiceIntegrationException(
                    "Order-service markOrderAsShipped failed. url=" + url
                            + ", status=" + ex.getStatusCode()
                            + ", body=" + ex.getResponseBodyAsString()
            );
        } catch (ResourceAccessException ex) {
            throw new OrderServiceIntegrationException(
                    "Order-service is unreachable at " + url + ". cause=" + ex.getMessage()
            );
        } catch (RestClientException ex) {
            throw new OrderServiceIntegrationException(
                    "Failed to mark order as shipped in order-service. cause=" + ex.getMessage()
            );
        }
    }
}