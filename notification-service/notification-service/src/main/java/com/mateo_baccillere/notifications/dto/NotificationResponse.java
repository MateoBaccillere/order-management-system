package com.mateo_baccillere.notifications.dto;


import java.time.LocalDateTime;


public class NotificationResponse {

    private Long id;
    private Long orderId;
    private String eventType;
    private String message;

    public NotificationResponse(Long id, Long orderId, String eventType, String message, LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.eventType = eventType;
        this.message = message;
        this.createdAt = createdAt;
    }

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
