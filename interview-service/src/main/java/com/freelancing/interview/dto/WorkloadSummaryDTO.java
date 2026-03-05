package com.freelancing.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class WorkloadSummaryDTO {
    private Long freelancerId;
    private Instant from;
    private Instant to;
    private long totalMinutes7;
    private long totalMinutes1;
    private int interviewsNext24h;
    private int interviewsNext3d;
    private int interviewsNext7d;
    private long maxDailyMinutes;
    private String level; // LIGHT, NORMAL, BUSY, OVERLOADED
}

