package com.nexaflow.orderservice.service;

import com.nexaflow.orderservice.dto.*;
import com.nexaflow.orderservice.model.*;
import com.nexaflow.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public OrderDto placeOrder(Long userId, PlaceOrderRequest request) {
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .build();

        List<OrderItem> items = request.getItems().stream().map(itemReq -> {
            BigDecimal subtotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            return OrderItem.builder()
                    .order(order)
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .subtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());

        order.setItems(items);
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.CONFIRMED);

        Order saved = orderRepository.save(order);

        List<OrderEvent.OrderItemSummary> itemSummaries = saved.getItems().stream()
                .map(i -> new OrderEvent.OrderItemSummary(i.getProductId(), i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .collect(Collectors.toList());

        OrderEvent event = new OrderEvent("ORDER_PLACED", saved.getId(), userId, total, itemSummaries, LocalDateTime.now());
        try {
            kafkaTemplate.send("order-events", String.valueOf(saved.getId()), event);
            log.info("Published ORDER_PLACED event for orderId={}", saved.getId());
        } catch (Exception e) {
            log.warn("Failed to publish ORDER_PLACED event: {}", e.getMessage());
        }

        return toDto(saved);
    }

    public List<OrderDto> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        return toDto(order);
    }

    @Transactional
    public OrderDto cancelOrder(Long orderId, Long userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to this user");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel a " + order.getStatus() + " order");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        Order saved = orderRepository.save(order);

        OrderEvent event = new OrderEvent("ORDER_CANCELLED", saved.getId(), userId, saved.getTotalAmount(), List.of(), LocalDateTime.now());
        try {
            kafkaTemplate.send("order-events", String.valueOf(saved.getId()), event);
            log.info("Published ORDER_CANCELLED event for orderId={}", saved.getId());
        } catch (Exception e) {
            log.warn("Failed to publish ORDER_CANCELLED event: {}", e.getMessage());
        }

        return toDto(saved);
    }

    public OrderStatsDto getOrderStats() {
        long total = orderRepository.count();
        long pending = orderRepository.countByStatus(OrderStatus.PENDING);
        long confirmed = orderRepository.countByStatus(OrderStatus.CONFIRMED);
        long shipped = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long delivered = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        BigDecimal revenue = orderRepository.sumRevenueFromDelivered(OrderStatus.DELIVERED);

        return OrderStatsDto.builder()
                .totalOrders(total)
                .pendingOrders(pending)
                .confirmedOrders(confirmed)
                .shippedOrders(shipped)
                .deliveredOrders(delivered)
                .cancelledOrders(cancelled)
                .totalRevenue(revenue)
                .build();
    }

    private OrderDto toDto(Order o) {
        List<OrderItemDto> itemDtos = o.getItems().stream().map(i -> OrderItemDto.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .unitPrice(i.getUnitPrice())
                .subtotal(i.getSubtotal())
                .build()).collect(Collectors.toList());

        return OrderDto.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .cancellationReason(o.getCancellationReason())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .items(itemDtos)
                .build();
    }
}
