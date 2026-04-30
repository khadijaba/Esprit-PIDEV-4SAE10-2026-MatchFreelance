package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AdaptiveRescheduleResponseDTO {
    private Long ownerId;
    private LocalDate weekStart;
    private Integer dailyCapacityMinutes;
    private List<DailyTaskAllocationDTO> allocations;
    private String aiSource;
}

