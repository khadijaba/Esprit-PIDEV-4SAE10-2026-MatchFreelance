package esprit.project.dto;

import esprit.project.entities.PhaseMeetingDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreatePhaseMeetingRequest {
    @NotNull
    private LocalDateTime meetingAt;
    private String agenda;
    private String summary;
    private PhaseMeetingDecision decision;
}
