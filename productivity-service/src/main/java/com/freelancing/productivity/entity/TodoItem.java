package com.freelancing.productivity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "todo_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "list_id", nullable = false)
    private Long listId;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @Column(name = "position_index", nullable = false)
    private Integer positionIndex;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.positionIndex == null || this.positionIndex < 0) {
            this.positionIndex = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

