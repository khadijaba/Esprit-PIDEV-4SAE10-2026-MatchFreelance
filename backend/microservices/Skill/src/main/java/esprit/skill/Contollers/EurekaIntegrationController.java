package esprit.skill.Contollers;

import esprit.skill.client.FormationClient;
import esprit.skill.client.ProjectClient;
import esprit.skill.client.UserActuatorClient;
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
    private final FormationClient formationClient;
    private final UserActuatorClient userActuatorClient;

    public EurekaIntegrationController(
            ProjectClient projectClient,
            FormationClient formationClient,
            UserActuatorClient userActuatorClient) {
        this.projectClient = projectClient;
        this.formationClient = formationClient;
        this.userActuatorClient = userActuatorClient;
    }

    @GetMapping("/eureka-peers")
    public Map<String, Object> pingPeers() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("service", "SKILL");
        out.put("via", "OpenFeign + Eureka");
        Map<String, String> peers = new LinkedHashMap<>();
        peers.put("PROJECT", safe(() -> {
            int n = projectClient.getAllProjects().size();
            return "OK (" + n + " projects)";
        }));
        peers.put("FORMATION", safe(() -> {
            int n = formationClient.getAllFormations().size();
            return "OK (" + n + " formations)";
        }));
        peers.put("USER", safe(() -> {
            userActuatorClient.health();
            return "UP (actuator/health)";
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
