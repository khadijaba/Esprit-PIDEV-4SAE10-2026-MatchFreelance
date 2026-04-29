package com.freelancing.candidature.client;

import com.freelancing.candidature.client.dto.CommunicationScorePayload;
import com.freelancing.candidature.client.dto.ContractApiResponse;
import com.freelancing.candidature.client.dto.ContractCreatePayload;
import feign.FeignException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class ContractClient {

    private final ContractRemoteFeign contractRemoteFeign;

    public ContractResponse createContract(ContractCreateRequest request) {
        ContractCreatePayload payload = new ContractCreatePayload();
        BeanUtils.copyProperties(request, payload);
        try {
            ContractApiResponse body = contractRemoteFeign.create(payload);
            return map(body);
        } catch (FeignException e) {
            String raw = e.contentUTF8();
            String detail =
                    (raw != null && !raw.isBlank())
                            ? raw
                            : (e.getMessage() != null ? e.getMessage() : "Erreur appel CONTRACT");
            throw new RuntimeException("Création contrat refusée (" + e.status() + "): " + detail);
        }
    }

    public ContractResponse getContractById(Long id) {
        try {
            return map(contractRemoteFeign.getById(id));
        } catch (Exception e) {
            return null;
        }
    }

    public ContractResponse payContract(Long id) {
        try {
            return map(contractRemoteFeign.pay(id));
        } catch (Exception e) {
            return null;
        }
    }

    public ContractResponse cancelContract(Long id) {
        try {
            return map(contractRemoteFeign.cancel(id));
        } catch (Exception e) {
            return null;
        }
    }

    public CommunicationScoreResponse getCommunicationScore(Long freelancerId) {
        if (freelancerId == null) {
            return new CommunicationScoreResponse(50.0, 0);
        }
        try {
            CommunicationScorePayload p = contractRemoteFeign.communicationScore(freelancerId);
            if (p == null) {
                return new CommunicationScoreResponse(50.0, 0);
            }
            return new CommunicationScoreResponse(p.getScore(), p.getMessageCount());
        } catch (Exception e) {
            return new CommunicationScoreResponse(50.0, 0);
        }
    }

    private static ContractResponse map(ContractApiResponse b) {
        if (b == null) {
            return null;
        }
        ContractResponse r = new ContractResponse();
        r.setId(b.getId());
        r.setProjectId(b.getProjectId());
        r.setFreelancerId(b.getFreelancerId());
        r.setClientId(b.getClientId());
        r.setTerms(b.getTerms());
        r.setStatus(b.getStatus());
        return r;
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
