package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyTaskAllocationDTO {
    private Long taskId;
    private String title;
    private LocalDate scheduledDate;
    private Integer allocatedMinutes;
}

