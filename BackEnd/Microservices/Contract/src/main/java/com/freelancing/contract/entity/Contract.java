package com.freelancing.contract.entity;

import com.freelancing.contract.enums.ContractStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(length = 4000)
    private String terms;

    @Column(name = "proposed_budget")
    private Double proposedBudget;

    @Column(name = "extra_tasks_budget")
    private Double extraTasksBudget;

    @Column(length = 2000)
    private String applicationMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    @Temporal(TemporalType.DATE)
    @Column(name = "start_date")
    private Date startDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "end_date")
    private Date endDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "pending_extra_amount")
    private Double pendingExtraAmount;

    @Column(name = "pending_extra_reason", length = 1000)
    private String pendingExtraReason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "pending_extra_requested_at")
    private Date pendingExtraRequestedAt;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "client_rating")
    private Integer clientRating;

    @Column(name = "client_review", length = 2000)
    private String clientReview;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "client_reviewed_at")
    private Date clientReviewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        if (status == null) status = ContractStatus.DRAFT;
    }
}
