package com.nexaflow.orderservice.controller;

import com.nexaflow.orderservice.dto.*;
import com.nexaflow.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<OrderDto> placeOrder(
            @RequestBody PlaceOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.parseLong(userIdHeader) : 1L;
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(userId, request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get orders for the current user")
    public ResponseEntity<List<OrderDto>> getMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.parseLong(userIdHeader) : 1L;
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderDto> cancelOrder(
            @PathVariable Long id,
            @RequestBody CancelOrderRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Long userId = userIdHeader != null ? Long.parseLong(userIdHeader) : 1L;
        return ResponseEntity.ok(orderService.cancelOrder(id, userId, request.getReason()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get order statistics (ADMIN only)")
    public ResponseEntity<OrderStatsDto> getOrderStats(
            @RequestHeader(value = "X-User-Role", defaultValue = "CUSTOMER") String userRole) {
        if (!"ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(orderService.getOrderStats());
    }
}
