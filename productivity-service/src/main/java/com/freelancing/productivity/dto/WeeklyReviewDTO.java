package com.freelancing.productivity.dto;

import lombok.Data;

import java.util.List;

@Data
public class WeeklyReviewDTO {
    private Long ownerId;
    private List<String> prompts;
}

