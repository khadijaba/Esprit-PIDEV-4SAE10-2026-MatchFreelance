package com.freelancing.productivity.dto;

import lombok.Data;

@Data
public class ProgressSummaryDTO {
    private Long ownerId;
    private long totalTasks;
    private long doneTasks;
    private long inProgressTasks;
    private long blockedTasks;
    private long overdueTasks;
    private double completionRate;
    private int totalPlannedMinutesOpen;
}

