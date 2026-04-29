package com.freelancing.candidature.client.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ContractCreatePayload {
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
