package com.freelancing.project.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractSummaryDTO {

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
