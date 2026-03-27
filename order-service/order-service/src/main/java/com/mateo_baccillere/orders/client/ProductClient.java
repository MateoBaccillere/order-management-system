package com.mateo_baccillere.orders.client;

import com.mateo_baccillere.orders.dto.ProductDetailsResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductClient {

    private final RestClient productRestClient;

    public ProductClient(@Qualifier("productRestClient") RestClient productRestClient) {
        this.productRestClient = productRestClient;
    }

    public ProductDetailsResponse getProductById(Long productId) {
        try {
            return productRestClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductDetailsResponse.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw ex;
        }
    }
}
