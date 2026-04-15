package esprit.project.dto;

import esprit.project.entities.PhaseMeetingDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreatePhaseMeetingRequest {
    /** ISO-8601 depuis le front (ex. {@code 2026-04-15T10:00:00.000Z}) — évite les 400 avec {@code LocalDateTime} + suffixe Z. */
    @NotNull
    private Instant meetingAt;
    private String agenda;
    private String summary;
    private PhaseMeetingDecision decision;
}
