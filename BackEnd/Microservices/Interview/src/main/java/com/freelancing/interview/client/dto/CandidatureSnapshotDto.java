package com.freelancing.interview.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidatureSnapshotDto {
    private Long id;
    private Long projectId;
    private Long freelancerId;
    private String status;
}
