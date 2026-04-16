package com.freelancing.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewMetricsDTO {

    private Integer interviewCount;
    private Boolean eligibleForAcceptance;
}
