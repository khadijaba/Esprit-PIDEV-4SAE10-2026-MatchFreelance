package esprit.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitFreelancerRatingRequest {

    @NotNull
    private Long freelancerId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;
}
