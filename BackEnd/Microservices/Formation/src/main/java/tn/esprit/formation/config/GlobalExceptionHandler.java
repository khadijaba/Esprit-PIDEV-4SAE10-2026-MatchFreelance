package tn.esprit.formation.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
<<<<<<< HEAD
=======
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Erreur"));
    }

<<<<<<< HEAD
=======
    /** Identifiant de path invalide (ex. /api/modules/allooo au lieu d'un nombre). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Identifiant invalide (nombre attendu).";
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", message, "message", message));
    }

>>>>>>> 8d5250d (Ajout du projet MatchFreelance)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handle404(NoHandlerFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", 404,
                        "error", "Not Found",
                        "path", ex.getRequestURL(),
                        "message", "Utilisez /api/formations ou /api/inscriptions. Page d'accueil: /api"
                ));
    }
}
