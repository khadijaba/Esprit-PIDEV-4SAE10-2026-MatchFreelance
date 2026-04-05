package com.freelancing.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ReliabilityResponseDTO {
    private Long userId;
    private String role; // "FREELANCER" or "OWNER"
    private double score; // 0.0 - 1.0
    private int completedCount;
    private int noShowCount;
    private int cancelledCount;
    private Instant from;
    private Instant to;
}

