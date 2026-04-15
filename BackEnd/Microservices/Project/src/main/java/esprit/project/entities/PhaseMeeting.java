package esprit.project.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "phase_meeting")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"phase"})
public class PhaseMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private ProjectPhase phase;

    @Column(nullable = false)
    private LocalDateTime meetingAt;

    @Column(length = 1000)
    private String agenda;

    @Column(length = 2000)
    private String summary;

    @Enumerated(EnumType.STRING)
    private PhaseMeetingDecision decision;
}
