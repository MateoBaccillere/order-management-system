

package com.mateo_baccillere.notifications.controller;

import com.mateo_baccillere.notifications.dto.NotificationRequest;
import com.mateo_baccillere.notifications.dto.NotificationResponse;
import com.mateo_baccillere.notifications.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {



    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse createNotification(@RequestBody NotificationRequest request) {
        return notificationService.createNotification(request);
    }

    @GetMapping
    public List<NotificationResponse> getAllNotifications() {
        return notificationService.getAllNotifications();
    }


}
