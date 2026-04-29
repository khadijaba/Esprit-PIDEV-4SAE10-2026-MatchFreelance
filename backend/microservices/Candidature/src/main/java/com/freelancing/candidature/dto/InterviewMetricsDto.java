package com.freelancing.candidature.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewMetricsDto {
    private Integer interviewCount;
    private Boolean eligibleForAcceptance;
}
