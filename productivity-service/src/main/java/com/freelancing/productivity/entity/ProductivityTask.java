package com.freelancing.productivity.entity;

import com.freelancing.productivity.enums.ProductivityPriority;
import com.freelancing.productivity.enums.ProductivityTaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "productivity_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "goal_id")
    private Long goalId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductivityTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductivityPriority priority;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "planned_minutes", nullable = false)
    private Integer plannedMinutes;

    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ProductivityTaskStatus.TODO;
        }
        if (this.priority == null) {
            this.priority = ProductivityPriority.MEDIUM;
        }
        if (this.plannedMinutes == null || this.plannedMinutes <= 0) {
            this.plannedMinutes = 30;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
