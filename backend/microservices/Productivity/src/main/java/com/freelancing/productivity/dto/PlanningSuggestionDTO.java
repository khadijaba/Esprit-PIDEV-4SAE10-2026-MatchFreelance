package com.freelancing.productivity.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PlanningSuggestionDTO {
    private Long taskId;
    private String title;
    private String priority;
    private Instant dueAt;
    private Integer plannedMinutes;
    private double score;
    private String rationale;
}

