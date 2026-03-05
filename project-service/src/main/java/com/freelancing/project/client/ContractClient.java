package com.freelancing.project.client;

import lombok.Data;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class ContractClient {

    private final RestTemplate restTemplate;
    private final String serviceUrl;

    public ContractClient(RestTemplate restTemplate, @org.springframework.beans.factory.annotation.Value("${contract.service.url:http://contract-service}") String serviceUrl) {
        this.restTemplate = restTemplate;
        this.serviceUrl = serviceUrl;
    }

    public List<ContractResponse> getContractsByProjectId(Long projectId) {
        try {
            return restTemplate.exchange(
                    serviceUrl + "/api/contracts/project/" + projectId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ContractResponse>>() {}
            ).getBody();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Data
    public static class ContractResponse {
        private Long id;
        private Long projectId;
        private Long freelancerId;
        private Long clientId;
        private String freelancerName;
        private String clientName;
        private String terms;
        private Double proposedBudget;
        private Double extraTasksBudget;
        private String applicationMessage;
        private String status;
        private Date startDate;
        private Date endDate;
        private Date createdAt;
        private Integer progressPercent;
        private Double pendingExtraAmount;
        private String pendingExtraReason;
        private Date pendingExtraRequestedAt;
        private Integer clientRating;
        private String clientReview;
        private Date clientReviewedAt;
    }
}
