package com.mateo_baccillere.orders.client;


import com.mateo_baccillere.orders.dto.NotificationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationClient {


    private final RestClient notificationRestClient;

    public NotificationClient(RestClient notificationRestClient) {
        this.notificationRestClient = notificationRestClient;
    }

    public void sendNotification(NotificationRequest request) {
        notificationRestClient.post()
                .uri("/api/notifications")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
