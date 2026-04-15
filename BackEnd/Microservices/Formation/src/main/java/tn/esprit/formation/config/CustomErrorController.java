package tn.esprit.formation.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Retourne du JSON pour /error au lieu de la page Whitelabel.
 * Utile quand une requête ne matche aucun controller (ex. proxy envoie un mauvais chemin).
 */
@RestController
public class CustomErrorController {

    @RequestMapping(value = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        if (statusCode == null) statusCode = 404;
        if (requestUri == null) requestUri = "";

        String message = statusCode == 404
                ? "Aucun mapping pour cette URL. Pour les modules : POST /api/modules, GET /api/modules/formation/{id}."
                : "Erreur " + statusCode;

        return ResponseEntity
                .status(HttpStatus.valueOf(statusCode))
                .body(Map.of(
                        "timestamp", java.time.Instant.now().toString(),
                        "status", statusCode,
                        "error", statusCode == 404 ? "Not Found" : "Error",
                        "path", requestUri,
                        "message", message
                ));
    }
}
