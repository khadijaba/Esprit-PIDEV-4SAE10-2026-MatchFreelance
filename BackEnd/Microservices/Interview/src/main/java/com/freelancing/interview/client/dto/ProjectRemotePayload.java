package com.freelancing.interview.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectRemotePayload {
    private Long id;
    private String title;
    private String description;
    private Double budget;
    private Integer duration;
    private String status;
    private Long projectOwnerId;
    private List<String> requiredSkills = new ArrayList<>();
}
