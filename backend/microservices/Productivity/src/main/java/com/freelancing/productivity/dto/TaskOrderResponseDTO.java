package com.freelancing.productivity.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskOrderResponseDTO {
    private Long ownerId;
    private List<Long> orderedTaskIds;
}

