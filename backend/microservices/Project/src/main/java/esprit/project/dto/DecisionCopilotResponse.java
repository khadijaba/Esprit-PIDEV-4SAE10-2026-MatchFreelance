package esprit.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DecisionCopilotResponse {
    private String recommendation;
    private Integer confidence;
    private String summary;
    private List<String> reasons;
    private List<String> suggestedActions;
    private String ownerMessageDraft;
}
