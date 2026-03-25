package com.nexaflow.notificationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexaflow.notificationservice.dto.NotificationDto;
import com.nexaflow.notificationservice.model.Notification;
import com.nexaflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void handleOrderEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.get("eventType").asText();
            long orderId = node.get("orderId").asLong();
            long userId = node.get("userId").asLong();
            double totalAmount = node.has("totalAmount") ? node.get("totalAmount").asDouble() : 0.0;

            Notification notification = null;

            if ("ORDER_PLACED".equals(eventType)) {
                notification = Notification.builder()
                        .userId(userId)
                        .type("ORDER_PLACED")
                        .title("Order Confirmed!")
                        .message(String.format("Your order #%d for $%.2f has been placed successfully.", orderId, totalAmount))
                        .read(false)
                        .createdAt(LocalDateTime.now())
                        .build();
            } else if ("ORDER_CANCELLED".equals(eventType)) {
                notification = Notification.builder()
                        .userId(userId)
                        .type("ORDER_CANCELLED")
                        .title("Order Cancelled")
                        .message(String.format("Your order #%d has been cancelled.", orderId))
                        .read(false)
                        .createdAt(LocalDateTime.now())
                        .build();
            }

            if (notification != null) {
                notificationRepository.save(notification);
                log.info("Saved {} notification for userId={}", eventType, userId);
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "product-events", groupId = "notification-service-group")
    public void handleProductEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.get("eventType").asText();
            String productName = node.has("productName") ? node.get("productName").asText() : "Unknown";
            int stock = node.has("stockQuantity") ? node.get("stockQuantity").asInt() : 0;

            if ("STOCK_LOW".equals(eventType) || "OUT_OF_STOCK".equals(eventType)) {
                String title = "OUT_OF_STOCK".equals(eventType) ? "Product Out of Stock" : "Low Stock Alert";
                String msg = "OUT_OF_STOCK".equals(eventType)
                        ? String.format("Product '%s' is now out of stock.", productName)
                        : String.format("Product '%s' has low stock (%d remaining).", productName, stock);

                Notification notification = Notification.builder()
                        .userId(0L) // admin notification
                        .type(eventType)
                        .title(title)
                        .message(msg)
                        .read(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                notificationRepository.save(notification);
                log.info("Saved {} notification for product={}", eventType, productName);
            }
        } catch (Exception e) {
            log.error("Error processing product event: {}", e.getMessage(), e);
        }
    }

    public List<NotificationDto> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public NotificationDto markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setRead(true);
        return toDto(notificationRepository.save(notification));
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
