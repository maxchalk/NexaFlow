package com.nexaflow.userservice.model;

import java.time.LocalDateTime;

public record UserEvent(
        String eventType,
        Long userId,
        String email,
        String firstName,
        LocalDateTime timestamp
) {}
