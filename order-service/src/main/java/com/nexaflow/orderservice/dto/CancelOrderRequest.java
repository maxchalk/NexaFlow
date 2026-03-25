package com.nexaflow.orderservice.dto;

import lombok.Data;

@Data
public class CancelOrderRequest {
    private String reason;
}
