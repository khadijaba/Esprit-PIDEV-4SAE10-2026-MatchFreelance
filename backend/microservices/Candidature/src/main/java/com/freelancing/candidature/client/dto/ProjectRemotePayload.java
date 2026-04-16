package com.freelancing.candidature.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente le JSON du microservice PROJECT (esprit) pour GET/PUT /projects/{id}.
 */
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
