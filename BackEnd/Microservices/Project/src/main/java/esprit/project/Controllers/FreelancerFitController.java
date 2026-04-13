package esprit.project.Controllers;

import esprit.project.Service.FreelancerFitService;
import esprit.project.dto.FreelancerFitBatchDto;
import esprit.project.dto.SubmitFreelancerRatingRequest;
import esprit.project.entities.ClientFreelancerRating;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/projects")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FreelancerFitController {

    private final FreelancerFitService freelancerFitService;

    /**
     * Estimation de durée + score de réussite pour chaque freelancer par rapport au projet cible.
     * Utilise les missions ACCEPTED passées (autres projets), skills, durées réelles (projets COMPLETED),
     * et la moyenne des notes {@link ClientFreelancerRating} si présentes.
     *
     * @param freelancerIds liste d’IDs séparés par des virgules (ex. 12,15,20)
     */
    @GetMapping("/{projectId}/freelancer-fit")
    public FreelancerFitBatchDto freelancerFit(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "") String freelancerIds) {
        List<Long> ids = new ArrayList<>();
        for (String part : freelancerIds.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(BAD_REQUEST, "freelancerIds invalid: " + s);
            }
        }
        return freelancerFitService.computeFit(projectId, ids);
    }

    /** Enregistre ou met à jour la note client (1–5) pour un freelancer sur un projet terminé. */
    @PostMapping("/{projectId}/freelancer-ratings")
    public ClientFreelancerRating submitRating(
            @PathVariable Long projectId,
            @Valid @RequestBody SubmitFreelancerRatingRequest body) {
        return freelancerFitService.submitRating(projectId, body);
    }
}
