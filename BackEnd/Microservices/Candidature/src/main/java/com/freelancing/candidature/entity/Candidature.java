package com.freelancing.candidature.entity;

import com.freelancing.candidature.enums.CandidatureStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "candidatures", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "freelancer_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(length = 2000)
    private String message;

    @Column(name = "proposed_budget")
    private Double proposedBudget;

    @Column(name = "extra_tasks_budget")
    private Double extraTasksBudget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidatureStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "ai_match_score")
    private Double aiMatchScore;

    @Column(name = "ai_insights", length = 2000)
    private String aiInsights;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        if (status == null) status = CandidatureStatus.PENDING;
    }
}
