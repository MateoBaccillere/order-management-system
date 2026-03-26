package com.mateo_baccillere.notifications.dto;



public class NotificationRequest {

    private Long orderId;
    private String eventType;
    private String message;

    public NotificationRequest(Long orderId, String eventType, String message) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.message = message;
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
}
