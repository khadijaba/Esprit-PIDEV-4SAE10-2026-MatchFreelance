package com.freelancing.productivity.dto;

import com.freelancing.productivity.enums.ProductivityPriority;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class TaskResponseDTO {
    private Long id;
    private Long ownerId;
    private Long goalId;
    private String title;
    private String description;
    private ProductivityTaskStatus status;
    private ProductivityPriority priority;
    private Integer plannedMinutes;
    private Integer actualMinutes;
    private Instant dueAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
