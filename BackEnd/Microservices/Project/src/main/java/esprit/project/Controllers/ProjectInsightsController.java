package esprit.project.Controllers;

import esprit.project.Service.ProjectInsightsService;
import esprit.project.dto.ProjectInsightsDto;
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
public class ProjectInsightsController {

    private final ProjectInsightsService projectInsightsService;

    /**
     * Scores, prédiction de recrutement et signaux de risque (heuristiques).
     * Le nombre de candidatures agrège via le microservice Candidature (appel interne Feign).
     */
    @GetMapping("/{projectId}/insights")
    public ProjectInsightsDto getInsights(@PathVariable Long projectId) {
        return projectInsightsService.computeInsights(projectId);
    }
}
