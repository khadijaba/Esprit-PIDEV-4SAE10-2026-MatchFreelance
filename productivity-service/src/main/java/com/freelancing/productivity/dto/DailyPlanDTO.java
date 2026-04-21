package com.freelancing.productivity.dto;

import lombok.Data;

import java.util.List;

@Data
public class DailyPlanDTO {
    private Long ownerId;
    private int focusMinutes;
    private int allocatedMinutes;
    private double utilizationRate;
    private double allocationScore;
    private String algorithmUsed;
    private List<PlanningSuggestionDTO> tasks;
}
