package esprit.project.Controllers;

import esprit.project.Service.ProjectEffortEstimateService;
import esprit.project.dto.ProjectEffortEstimateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/projects")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProjectEffortEstimateController {

    private final ProjectEffortEstimateService projectEffortEstimateService;

    /** Estimation jours-homme indicative (heuristique texte + compétences + mots-clés complexité). */
    @GetMapping("/{projectId}/effort-estimate")
    public ProjectEffortEstimateDto getEffortEstimate(@PathVariable Long projectId) {
        return projectEffortEstimateService.estimate(projectId);
    }
}
