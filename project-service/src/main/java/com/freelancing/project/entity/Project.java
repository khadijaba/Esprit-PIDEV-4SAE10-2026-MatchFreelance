package com.freelancing.project.entity;

import com.freelancing.project.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "min_budget", nullable = false)
    private Double minBudget;

    @Column(name = "max_budget", nullable = false)
    private Double maxBudget;

    @Column(nullable = false)
    private Integer duration;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(name = "client_id")
    private Long clientId;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        if (status == null) {
            status = ProjectStatus.OPEN;
        }
    }
}
