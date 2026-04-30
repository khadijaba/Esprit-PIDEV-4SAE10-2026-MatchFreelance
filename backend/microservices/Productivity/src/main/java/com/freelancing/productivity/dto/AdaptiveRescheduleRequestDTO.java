package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AdaptiveRescheduleRequestDTO {
    private LocalDate weekStart;
    private Integer dailyCapacityMinutes;
}

