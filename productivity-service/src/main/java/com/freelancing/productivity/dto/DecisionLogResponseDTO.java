package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class DecisionLogResponseDTO {
    private Long id;
    private Long ownerId;
    private Long taskId;
    private String decisionType;
    private String reason;
    private Instant createdAt;
}

