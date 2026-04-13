package esprit.project.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class FreelancerFitBatchDto {
    Long projectId;
    List<FreelancerFitDto> freelancers;
}
