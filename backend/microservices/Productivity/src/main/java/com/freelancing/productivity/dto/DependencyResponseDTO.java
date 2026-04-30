package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class DependencyResponseDTO {
    private Long id;
    private Long ownerId;
    private Long predecessorTaskId;
    private Long successorTaskId;
    private Instant createdAt;
}

