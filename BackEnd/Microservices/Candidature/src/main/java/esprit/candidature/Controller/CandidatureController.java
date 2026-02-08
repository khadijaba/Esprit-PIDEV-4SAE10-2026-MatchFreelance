package esprit.candidature.Controller;



import esprit.candidature.entities.Candidature;
import esprit.candidature.service.CandidatureService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/candidature")
@CrossOrigin(origins = "*")
public class CandidatureController {

    private final CandidatureService candidatureService;

    public CandidatureController(CandidatureService candidatureService) {
        this.candidatureService = candidatureService;
    }

    // Ajouter une candidature
    @PostMapping("/add")
    public Candidature addCandidature(
            @RequestParam Long freelancerId,
            @RequestParam Long projectId
    ) {
        return candidatureService.addCandidature(freelancerId, projectId);
    }

    // Récupérer toutes les candidatures
    @GetMapping("/all")
    public List<Candidature> getAll() {
        return candidatureService.getAll();
    }

    // Récupérer candidatures d’un freelance
    @GetMapping("/freelancer/{freelancerId}")
    public List<Candidature> getByFreelancer(@PathVariable Long freelancerId) {
        return candidatureService.getByFreelancer(freelancerId);
    }

    // Mettre à jour le statut
    @PutMapping("/update/{id}")
    public Candidature updateStatut(
            @PathVariable Long id,
            @RequestParam String statut
    ) throws Exception {
        return candidatureService.updateStatut(id, statut);
    }

    // Supprimer une candidature
    @DeleteMapping("/delete/{id}")
    public void deleteCandidature(@PathVariable Long id) {
        candidatureService.deleteCandidature(id);
    }
}
