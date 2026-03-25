package com.nexaflow.orderservice.service;

import com.nexaflow.orderservice.dto.CancelOrderRequest;
import com.nexaflow.orderservice.dto.OrderDto;
import com.nexaflow.orderservice.dto.OrderItemRequest;
import com.nexaflow.orderservice.dto.PlaceOrderRequest;
import com.nexaflow.orderservice.model.Order;
import com.nexaflow.orderservice.model.OrderItem;
import com.nexaflow.orderservice.model.OrderStatus;
import com.nexaflow.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks private OrderService orderService;

    private Order confirmedOrder;
    private Order cancelledOrder;

    @BeforeEach
    void setUp() {
        confirmedOrder = Order.builder()
                .id(1L)
                .userId(10L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("99.99"))
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        cancelledOrder = Order.builder()
                .id(2L)
                .userId(10L)
                .status(OrderStatus.CANCELLED)
                .totalAmount(new BigDecimal("50.00"))
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void placeOrder_success() {
        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId(1L);
        itemReq.setProductName("Widget");
        itemReq.setQuantity(2);
        itemReq.setUnitPrice(new BigDecimal("49.99"));

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setItems(List.of(itemReq));

        Order savedOrder = Order.builder()
                .id(1L)
                .userId(10L)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("99.98"))
                .items(List.of(OrderItem.builder()
                        .id(1L)
                        .productId(1L)
                        .productName("Widget")
                        .quantity(2)
                        .unitPrice(new BigDecimal("49.99"))
                        .subtotal(new BigDecimal("99.98"))
                        .build()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderDto result = orderService.placeOrder(10L, request);

        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED.name(), result.getStatus());
        verify(kafkaTemplate).send(eq("order-events"), anyString(), any());
    }

    @Test
    void cancelOrder_success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(confirmedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(confirmedOrder);

        OrderDto result = orderService.cancelOrder(1L, 10L, "Changed mind");

        assertEquals(OrderStatus.CANCELLED.name(), result.getStatus());
        verify(kafkaTemplate).send(eq("order-events"), anyString(), any());
    }

    @Test
    void cancelOrder_alreadyCancelled_throws() {
        when(orderRepository.findById(2L)).thenReturn(Optional.of(cancelledOrder));
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(2L, 10L, "reason"));
    }
}
