package com.nexaflow.notificationservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    private Long userId;
    private String type;
    private String title;
    private String message;

    @Builder.Default
    private boolean read = false;

    private LocalDateTime createdAt;
}
