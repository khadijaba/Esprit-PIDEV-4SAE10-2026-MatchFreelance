package esprit.skill.Contollers;

import esprit.skill.Service.FreelancerBioService;
import esprit.skill.dto.BioRequest;
import esprit.skill.entities.FreelancerBio;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bio")
@CrossOrigin(origins = "*")
public class FreelancerBioController {

    private final FreelancerBioService bioService;

    public FreelancerBioController(FreelancerBioService bioService) {
        this.bioService = bioService;
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ResponseEntity<Map<String, Object>> getBio(@PathVariable("freelancerId") Long freelancerId) {
        FreelancerBio b = bioService.getOrEmpty(freelancerId);
        return ResponseEntity.ok(Map.of(
            "id", b.getId() != null ? b.getId() : 0,
            "freelancerId", b.getFreelancerId(),
            "bio", b.getBio() != null ? b.getBio() : ""
        ));
    }

    @PutMapping("/freelancer/{freelancerId}")
    public ResponseEntity<Map<String, Object>> saveBio(
            @PathVariable("freelancerId") Long freelancerId,
            @RequestBody @Valid BioRequest request) {
        FreelancerBio saved = bioService.save(freelancerId, request.getBio());
        return ResponseEntity.ok(Map.of(
            "id", saved.getId() != null ? saved.getId() : 0,
            "freelancerId", saved.getFreelancerId(),
            "bio", saved.getBio() != null ? saved.getBio() : ""
        ));
    }

    @DeleteMapping("/freelancer/{freelancerId}")
    public ResponseEntity<Void> deleteBio(@PathVariable("freelancerId") Long freelancerId) {
        bioService.delete(freelancerId);
        return ResponseEntity.noContent().build();
    }
}
