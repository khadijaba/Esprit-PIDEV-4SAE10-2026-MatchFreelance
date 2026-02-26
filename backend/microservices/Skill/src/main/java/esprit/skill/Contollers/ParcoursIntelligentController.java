package esprit.skill.Contollers;

import esprit.skill.Service.ParcoursIntelligentService;
import esprit.skill.dto.ParcoursIntelligentResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/skills/parcours")
@CrossOrigin(origins = "*")
public class ParcoursIntelligentController {

    private final ParcoursIntelligentService parcoursIntelligentService;

    public ParcoursIntelligentController(ParcoursIntelligentService parcoursIntelligentService) {
        this.parcoursIntelligentService = parcoursIntelligentService;
    }

    /**
     * Parcours personnalisé : analyse compétences actuelles, détection des gaps,
     * proposition de formations ciblées (microservice Formation).
     */
    @GetMapping("/intelligent")
    public ParcoursIntelligentResponse getParcoursIntelligent(
            @RequestParam Long freelancerId) {
        return parcoursIntelligentService.calculerParcours(freelancerId);
    }
}
