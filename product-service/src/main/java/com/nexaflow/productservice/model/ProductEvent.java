package com.nexaflow.productservice.model;

import java.time.LocalDateTime;

public record ProductEvent(
        String eventType,
        Long productId,
        String productName,
        Integer stockQuantity,
        LocalDateTime timestamp
) {}
