package com.mateo_baccillere.orders.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfiguration {

    @Bean
    public RestClient notificationRestClient(
            @Value("${notification.service.url}") String notificationServiceUrl
    ) {
        return RestClient.builder()
                .baseUrl(notificationServiceUrl)
                .build();
    }
}
