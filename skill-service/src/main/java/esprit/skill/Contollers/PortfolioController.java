package esprit.skill.Contollers;

import esprit.skill.Service.PortfolioService;
import esprit.skill.entities.Portfolio;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @PostMapping("/{freelancerId}")
    public Portfolio addPortfolio(
            @PathVariable("freelancerId") Long freelancerId,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) throw new RuntimeException("Body required");
        Object urlObj = body.get("portfolioUrl");
        String url = urlObj != null ? urlObj.toString().trim() : null;
        Object descObj = body.get("portfolioDescription");
        String desc = (descObj != null && !descObj.toString().isBlank()) ? descObj.toString().trim() : null;
        if (url == null || url.isEmpty()) throw new RuntimeException("portfolioUrl is required");
        return portfolioService.addPortfolio(freelancerId, url, desc);
    }

    @GetMapping("/freelancer/{freelancerId}")
    public List<Portfolio> getPortfolios(@PathVariable("freelancerId") Long freelancerId) {
        return portfolioService.getPortfoliosByFreelancer(freelancerId);
    }

    @PutMapping("/{id}")
    public Portfolio updatePortfolio(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) throw new RuntimeException("Body required");
        Object urlObj = body.get("portfolioUrl");
        String url = urlObj != null ? urlObj.toString().trim() : null;
        Object descObj = body.get("portfolioDescription");
        String desc = (descObj != null && !descObj.toString().isBlank()) ? descObj.toString().trim() : null;
        if (url == null || url.isEmpty()) throw new RuntimeException("portfolioUrl is required");
        return portfolioService.updatePortfolio(id, url, desc);
    }

    @DeleteMapping("/{id}")
    public void deletePortfolio(@PathVariable("id") Long id) {
        portfolioService.deletePortfolio(id);
    }

    @GetMapping("/all")
    public List<Portfolio> getAllPortfolios() {
        return portfolioService.getAllPortfolios();
    }
}
