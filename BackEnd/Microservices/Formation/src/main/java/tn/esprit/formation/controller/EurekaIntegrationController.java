package tn.esprit.formation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.formation.client.ProjectClient;
import tn.esprit.formation.client.SkillClient;
import tn.esprit.formation.client.UserActuatorClient;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/integration")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EurekaIntegrationController {

    private final ProjectClient projectClient;
    private final SkillClient skillClient;
    private final UserActuatorClient userActuatorClient;

    @GetMapping("/eureka-peers")
    public Map<String, Object> pingPeers() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("service", "FORMATION");
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
