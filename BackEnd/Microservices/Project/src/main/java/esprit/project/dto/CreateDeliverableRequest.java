package esprit.project.dto;

import esprit.project.entities.DeliverableType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDeliverableRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private DeliverableType type;
}
