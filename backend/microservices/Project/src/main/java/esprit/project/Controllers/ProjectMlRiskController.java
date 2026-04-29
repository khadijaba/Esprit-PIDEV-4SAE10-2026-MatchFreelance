package esprit.project.Controllers;

import esprit.project.Service.ProjectMlRiskService;
import esprit.project.dto.ProjectMlRiskDto;
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
public class ProjectMlRiskController {

    private final ProjectMlRiskService projectMlRiskService;

    /** Score de risque projet : forêt aléatoire ONNX (entraînée via {@code ml/train_project_risk.py}) ou repli heuristique. */
    @GetMapping("/{projectId}/risk-ml")
    public ProjectMlRiskDto getMlRisk(@PathVariable Long projectId) {
        return projectMlRiskService.evaluate(projectId);
    }
}
