package esprit.skill.Contollers;


import esprit.skill.Service.CVService;
import esprit.skill.entities.CV;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/cv")
@CrossOrigin(origins = "*")
public class CVController {

    private final CVService cvService;

    public CVController(CVService cvService) {
        this.cvService = cvService;
    }

    // 1️⃣ Upload d'un CV pour un freelance
    @PostMapping("/upload/{freelancerId}")
    public CV uploadCV(@PathVariable("freelancerId") Long freelancerId,
                       @RequestParam("file") MultipartFile file) {
        try {
            return cvService.uploadCV(freelancerId, file);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Failed to upload CV");
        }
    }

    // 2️⃣ Récupérer le CV d'un freelance (404 si absent)
    @GetMapping("/freelancer/{freelancerId}")
    public org.springframework.http.ResponseEntity<CV> getCV(@PathVariable("freelancerId") Long freelancerId) {
        return cvService.getCVByFreelancerOptional(freelancerId)
                .map(org.springframework.http.ResponseEntity::ok)
                .orElse(org.springframework.http.ResponseEntity.notFound().build());
    }

    // 3️⃣ Supprimer le CV d'un freelance
    @DeleteMapping("/freelancer/{freelancerId}")
    public ResponseEntity<Void> deleteCV(@PathVariable("freelancerId") Long freelancerId) {
        cvService.deleteCV(freelancerId);
        return ResponseEntity.noContent().build();
    }

    // 4️⃣ Récupérer tous les CVs (pour supervision admin)
    @GetMapping("/all")
    public List<CV> getAllCVs() {
        return cvService.getAllCVs();
    }
}
