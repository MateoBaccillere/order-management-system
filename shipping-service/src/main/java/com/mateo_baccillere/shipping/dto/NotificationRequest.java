package com.mateo_baccillere.shipping.dto;

public record NotificationRequest(
        Long referenceId,
        String type,
        String message
) {
}
