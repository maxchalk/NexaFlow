package com.nexaflow.orderservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderEvent(
        String eventType,
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        List<OrderItemSummary> items,
        LocalDateTime timestamp
) {
    public record OrderItemSummary(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}
