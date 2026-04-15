package com.freelancing.candidature.client;

import lombok.Data;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@Component
public class ContractClient {

    private static final String SERVICE_URL = "http://contract-service";

    private final RestTemplate restTemplate;

    public ContractClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ContractResponse createContract(ContractCreateRequest request) {
        return restTemplate.postForObject(SERVICE_URL + "/api/contracts", request, ContractResponse.class);
    }

    public ContractResponse getContractById(Long id) {
        return restTemplate.getForObject(SERVICE_URL + "/api/contracts/" + id, ContractResponse.class);
    }

    public ContractResponse payContract(Long id) {
        return restTemplate.exchange(SERVICE_URL + "/api/contracts/" + id + "/pay",
                org.springframework.http.HttpMethod.PUT, null, ContractResponse.class).getBody();
    }

    /** Client-initiated cancel (must match contract.clientId on contract-service). */
    public ContractResponse cancelContract(Long id, Long clientId) {
        ContractCancelPartyRequest body = new ContractCancelPartyRequest();
        body.setClientId(clientId);
        return exchangeCancel(id, body);
    }

    /** Freelancer-initiated cancel (must match contract.freelancerId on contract-service). */
    public ContractResponse cancelContractAsFreelancer(Long id, Long freelancerId) {
        ContractCancelPartyRequest body = new ContractCancelPartyRequest();
        body.setFreelancerId(freelancerId);
        return exchangeCancel(id, body);
    }

    private ContractResponse exchangeCancel(Long id, ContractCancelPartyRequest body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ContractCancelPartyRequest> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(
                SERVICE_URL + "/api/contracts/" + id + "/cancel",
                HttpMethod.PUT,
                entity,
                ContractResponse.class).getBody();
    }

    /**
     * Returns chat communication score (0–100) for a freelancer based on their message history.
     * Returns null or default score 50 on failure / no data.
     */
    public CommunicationScoreResponse getCommunicationScore(Long freelancerId) {
        if (freelancerId == null) return new CommunicationScoreResponse(50.0, 0);
        try {
            CommunicationScoreResponse body = restTemplate.getForObject(
                    SERVICE_URL + "/api/contracts/freelancer/" + freelancerId + "/communication-score",
                    CommunicationScoreResponse.class);
            return body != null ? body : new CommunicationScoreResponse(50.0, 0);
        } catch (Exception e) {
            return new CommunicationScoreResponse(50.0, 0);
        }
    }

    @Data
    public static class CommunicationScoreResponse {
        private double score;
        private int messageCount;

        public CommunicationScoreResponse() {}

        public CommunicationScoreResponse(double score, int messageCount) {
            this.score = score;
            this.messageCount = messageCount;
        }
    }

    @Data
    public static class ContractCancelPartyRequest {
        private Long clientId;
        private Long freelancerId;
    }

    @Data
    public static class ContractCreateRequest {
        private Long projectId;
        private Long freelancerId;
        private Long clientId;
        private String terms;
        private Double proposedBudget;
        private Double extraTasksBudget;
        private String applicationMessage;
        private String status;
        private Date startDate;
        private Date endDate;
    }

    @Data
    public static class ContractResponse {
        private Long id;
        private Long projectId;
        private Long freelancerId;
        private Long clientId;
        private String terms;
        private String status;
    }
}
