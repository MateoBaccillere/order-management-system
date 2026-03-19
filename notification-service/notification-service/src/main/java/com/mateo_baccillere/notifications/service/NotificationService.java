package com.mateo_baccillere.notifications.service;

import com.mateo_baccillere.notifications.dto.NotificationRequest;
import com.mateo_baccillere.notifications.dto.NotificationResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NotificationService {

    private final List<NotificationResponse> notifications = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public NotificationResponse createNotification(NotificationRequest request) {
        NotificationResponse notification = new NotificationResponse(
                idGenerator.getAndIncrement(),
                request.getOrderId(),
                request.getEventType(),
                request.getMessage(),
                LocalDateTime.now()
        );

        notifications.add(notification);
        return notification;
    }

    public List<NotificationResponse> getAllNotifications() {
        return notifications;
    }
}
