package com.mateo_baccillere.products.client;

import com.mateo_baccillere.products.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${services.user-service.base-url}")
    private String userServiceBaseUrl;

    public UserResponse getUserById(Long userId) {
        try {
            return restTemplate.getForObject(
                    userServiceBaseUrl + "/api/users/" + userId,
                    UserResponse.class
            );
        } catch (RestClientException ex) {
            return null;
        }
    }
}
