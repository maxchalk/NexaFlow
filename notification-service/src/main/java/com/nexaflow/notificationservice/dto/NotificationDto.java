package com.nexaflow.notificationservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private String id;
    private Long userId;
    private String type;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}
