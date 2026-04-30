package com.freelancing.productivity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "decision_log_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "decision_type", nullable = false, length = 80)
    private String decisionType;

    @Column(name = "reason", nullable = false, length = 4000)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

