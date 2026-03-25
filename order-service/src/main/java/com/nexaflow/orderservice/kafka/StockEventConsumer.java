package com.nexaflow.orderservice.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product-events", groupId = "order-service-group")
    public void handleProductEvent(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.get("eventType").asText();
            String productName = node.has("productName") ? node.get("productName").asText() : "Unknown";
            log.info("Received product event: type={}, product={}", eventType, productName);

            if ("OUT_OF_STOCK".equals(eventType)) {
                log.warn("Product {} is out of stock", productName);
            } else if ("STOCK_LOW".equals(eventType)) {
                log.warn("Product {} has low stock", productName);
            }
        } catch (Exception e) {
            log.error("Error processing product event: {}", e.getMessage());
        }
    }
}
