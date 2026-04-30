package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class TodoListResponseDTO {
    private Long id;
    private Long ownerId;
    private String name;
    private long totalItems;
    private long completedItems;
    private Instant createdAt;
}

