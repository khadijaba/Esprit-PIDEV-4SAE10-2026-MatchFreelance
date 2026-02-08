package esprit.skill.Contollers;


import esprit.skill.Service.CVService;
import esprit.skill.entities.CV;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/cv")
@CrossOrigin(origins = "*")
public class CVController {

    private final CVService cvService;

    public CVController(CVService cvService) {
        this.cvService = cvService;
    }

    // 1️⃣ Upload d’un CV pour un freelance
    @PostMapping("/upload/{freelancerId}")
    public CV uploadCV(
            @PathVariable Long freelancerId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        return cvService.uploadCV(freelancerId, file);
    }

    // 2️⃣ Récupérer le CV d’un freelance
    @GetMapping("/freelancer/{freelancerId}")
    public CV getCV(@PathVariable Long freelancerId) {
        return cvService.getCVByFreelancer(freelancerId);
    }

    // 3️⃣ Supprimer le CV d’un freelance
    @DeleteMapping("/freelancer/{freelancerId}")
    public void deleteCV(@PathVariable Long freelancerId) {
        cvService.deleteCV(freelancerId);
    }
}

