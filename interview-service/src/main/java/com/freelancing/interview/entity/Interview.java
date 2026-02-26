package com.freelancing.interview.entity;

import com.freelancing.interview.enums.InterviewStatus;
import com.freelancing.interview.enums.MeetingMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "interviews",
        indexes = {
                @Index(name = "idx_interview_freelancer_start", columnList = "freelancer_id,start_at"),
                @Index(name = "idx_interview_owner_start", columnList = "owner_id,start_at"),
                @Index(name = "idx_interview_status", columnList = "status")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidature_id")
    private Long candidatureId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "freelancer_id", nullable = false)
    private Long freelancerId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingMode mode;

    // ONLINE
    @Column(name = "meeting_url", length = 2000)
    private String meetingUrl;

    // FACE_TO_FACE
    @Column(name = "address_line", length = 500)
    private String addressLine;

    @Column(length = 200)
    private String city;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status;

    @Column(length = 2000)
    private String notes;

    /** Optional in-app visio room id (for embed); when set, join URL can be derived or stored. */
    @Column(name = "visio_room_id", length = 100)
    private String visioRoomId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (status == null) status = InterviewStatus.PROPOSED;
        if (mode == null) mode = MeetingMode.ONLINE;
    }
}

