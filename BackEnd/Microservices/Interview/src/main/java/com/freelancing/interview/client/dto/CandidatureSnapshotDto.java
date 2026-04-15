package com.freelancing.interview.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidatureSnapshotDto {
    private Long id;

    @JsonAlias({ "project_id" })
    private Long projectId;

    @JsonAlias({ "freelancer_id" })
    private Long freelancerId;

    private String status;
}
