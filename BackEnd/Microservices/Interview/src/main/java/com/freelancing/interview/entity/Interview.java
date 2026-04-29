package com.freelancing.interview.entity;

import com.freelancing.interview.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "interviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidature_id", nullable = false)
    private Long candidatureId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "scheduled_at", nullable = false)
    private Date scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    @Column(length = 2000)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = InterviewStatus.SCHEDULED;
        }
    }
}
