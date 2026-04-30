package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class TodoItemResponseDTO {
    private Long id;
    private Long ownerId;
    private Long listId;
    private String title;
    private boolean done;
    private Integer positionIndex;
    private Instant dueAt;
    private Instant createdAt;
    private Instant updatedAt;
}

