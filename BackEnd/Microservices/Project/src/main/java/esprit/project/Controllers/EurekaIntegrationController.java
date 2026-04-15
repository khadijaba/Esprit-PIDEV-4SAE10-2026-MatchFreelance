package esprit.project.Controllers;

import esprit.project.client.FormationClient;
import esprit.project.client.SkillClient;
import esprit.project.client.UserActuatorClient;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vérifie les appels OpenFeign vers les autres services enregistrés sur Eureka.
 */
@RestController
@RequestMapping("/integration")
@CrossOrigin(origins = "*")
public class EurekaIntegrationController {

    private final UserActuatorClient userActuatorClient;
    private final SkillClient skillClient;
    private final FormationClient formationClient;

    public EurekaIntegrationController(
            UserActuatorClient userActuatorClient,
            SkillClient skillClient,
            FormationClient formationClient) {
        this.userActuatorClient = userActuatorClient;
        this.skillClient = skillClient;
        this.formationClient = formationClient;
    }

    @GetMapping("/eureka-peers")
    public Map<String, Object> pingPeers() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("service", "PROJECT");
        out.put("via", "OpenFeign + Eureka");
        Map<String, String> peers = new LinkedHashMap<>();
        peers.put("USER", safe(() -> {
            userActuatorClient.health();
            return "UP (actuator/health)";
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
