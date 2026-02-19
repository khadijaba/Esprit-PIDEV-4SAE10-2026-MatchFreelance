package tn.esprit.formation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Evite 404 sur la racine et donne les liens de l'API.
 */
@RestController
public class WelcomeController {

    @GetMapping({"/", "/api"})
    public ResponseEntity<Map<String, Object>> welcome() {
        return ResponseEntity.ok(Map.of(
                "service", "Formation",
                "description", "Microservice Formation - Gestion des formations freelancers",
                "links", Map.of(
                        "formations", "/api/formations",
                        "formations_ouvertes", "/api/formations/ouvertes",
                        "inscriptions", "/api/inscriptions",
                        "health", "/actuator/health"
                )
        ));
    }
}
