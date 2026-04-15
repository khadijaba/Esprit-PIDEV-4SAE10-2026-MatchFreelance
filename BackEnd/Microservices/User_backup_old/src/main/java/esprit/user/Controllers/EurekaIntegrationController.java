package esprit.user.Controllers;

import esprit.user.client.FormationClient;
import esprit.user.client.ProjectClient;
import esprit.user.client.SkillClient;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/integration")
@CrossOrigin(origins = "*")
public class EurekaIntegrationController {

    private final ProjectClient projectClient;
    private final SkillClient skillClient;
    private final FormationClient formationClient;

    public EurekaIntegrationController(
            ProjectClient projectClient,
            SkillClient skillClient,
            FormationClient formationClient) {
        this.projectClient = projectClient;
        this.skillClient = skillClient;
        this.formationClient = formationClient;
    }

    @GetMapping("/eureka-peers")
    public Map<String, Object> pingPeers() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("service", "USER");
        out.put("via", "OpenFeign + Eureka");
        Map<String, String> peers = new LinkedHashMap<>();
        peers.put("PROJECT", safe(() -> {
            int n = projectClient.getAllProjects().size();
            return "OK (" + n + " projects)";
        }));
        peers.put("SKILL", safe(() -> {
            int n = skillClient.getAllSkills().size();
            return "OK (" + n + " skills)";
        }));
        peers.put("FORMATION", safe(() -> {
            int n = formationClient.getAllFormations().size();
            return "OK (" + n + " formations)";
        }));
        out.put("peers", peers);
        return out;
    }

    private static String safe(SafeCall call) {
        try {
            return call.run();
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }

    @FunctionalInterface
    private interface SafeCall {
        String run() throws Exception;
    }
}
