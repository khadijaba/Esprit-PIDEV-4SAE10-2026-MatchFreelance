package com.freelancing.productivity.dto;

import lombok.Data;

@Data
public class ProductivityInsightsDTO {
    private Long ownerId;
    private double estimationAccuracyScore;
    private double completionRate;
    private int currentCompletionStreakDays;
    private int bestPerformanceHour;
    private int worstPerformanceHour;
}

