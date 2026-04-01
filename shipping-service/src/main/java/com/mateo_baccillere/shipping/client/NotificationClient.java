package com.mateo_baccillere.shipping.client;

import com.mateo_baccillere.shipping.dto.NotificationRequest;
import com.mateo_baccillere.shipping.exception.NotificationServiceIntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String notificationServiceBaseUrl;

    public NotificationClient(
            RestTemplate restTemplate,
            @Value("${clients.notification-service.base-url}") String notificationServiceBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.notificationServiceBaseUrl = notificationServiceBaseUrl;
    }

    public void sendNotification(NotificationRequest request) {
        String url = notificationServiceBaseUrl + "/api/notifications";

        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (RestClientException ex) {
            throw new NotificationServiceIntegrationException(
                    "Failed to send notification to notification-service"
            );
        }
    }
}
