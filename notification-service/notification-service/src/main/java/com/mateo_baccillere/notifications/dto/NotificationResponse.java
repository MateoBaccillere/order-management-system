package com.mateo_baccillere.notifications.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {

    private Long id;
    private Long orderId;
    private String eventType;
    private String message;
    private LocalDateTime createdAt;

}
