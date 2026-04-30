package com.freelancing.productivity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "task_dependencies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dependency_edge", columnNames = {"owner_id", "predecessor_task_id", "successor_task_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "predecessor_task_id", nullable = false)
    private Long predecessorTaskId;

    @Column(name = "successor_task_id", nullable = false)
    private Long successorTaskId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}

