package com.freelancing.project.dto;

import com.freelancing.project.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponseDTO {

    private Long id;
    private String title;
    private String description;
    private Double minBudget;
    private Double maxBudget;
    private Integer duration;
    private Date createdAt;
    private ProjectStatus status;
    private Long clientId;
    private String clientName;
    private List<ContractSummaryDTO> contracts;
}

