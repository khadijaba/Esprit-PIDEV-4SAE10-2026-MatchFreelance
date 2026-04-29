package esprit.project.dto;

import esprit.project.entities.DeliverableReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewDeliverableRequest {
    @NotNull
    private DeliverableReviewStatus reviewStatus;
    private String reviewComment;
}
