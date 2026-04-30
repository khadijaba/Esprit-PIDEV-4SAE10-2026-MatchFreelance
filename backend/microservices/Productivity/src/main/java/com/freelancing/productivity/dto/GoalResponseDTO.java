package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class GoalResponseDTO {
    private Long id;
    private Long ownerId;
    private String title;
    private String description;
    private Instant targetDate;
    private long totalTasks;
    private long doneTasks;
    private double completionRate;
    private double weeklyVelocity;
}

