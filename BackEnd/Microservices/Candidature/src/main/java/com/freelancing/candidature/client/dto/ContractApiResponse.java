package com.freelancing.candidature.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractApiResponse {
    private Long id;
    private Long projectId;
    private Long freelancerId;
    private Long clientId;
    private String terms;
    private String status;
}
