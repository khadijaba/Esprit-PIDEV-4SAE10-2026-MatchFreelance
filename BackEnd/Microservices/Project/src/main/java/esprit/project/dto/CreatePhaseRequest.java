package esprit.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreatePhaseRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull
    private Integer phaseOrder;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
}
