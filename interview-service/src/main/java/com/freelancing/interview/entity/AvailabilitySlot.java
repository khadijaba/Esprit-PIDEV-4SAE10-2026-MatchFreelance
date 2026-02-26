package com.freelancing.interview.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "availability_slots",
        indexes = {
                @Index(name = "idx_slot_freelancer_start", columnList = "freelancer_id,start_at"),
                @Index(name = "idx_slot_booked", columnList = "booked")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(nullable = false)
    private boolean booked = false;

    @Column(name = "booked_interview_id")
    private Long bookedInterviewId;

    @PrePersist
    protected void onCreate() {
        if (bookedInterviewId != null) booked = true;
    }
}

