package esprit.tn.blog.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InappropriateContentException.class)
    public ResponseEntity<Map<String, Object>> handleInappropriateContent(InappropriateContentException e) {
        log.warn("Inappropriate content detected: {}", e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "MODERATION_ERROR");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("error", "VALIDATION_ERROR");
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("not found")) {
            log.warn("Resource not found: {}", message);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "NOT_FOUND");
            response.put("message", message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        log.error("Unhandled runtime exception: {}", message, e);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "RUNTIME_ERROR");
        response.put("message", message);
        response.put("details", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected exception: {}", e.getMessage(), e);
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INTERNAL_ERROR");
        response.put("message", "Database error: " + e.getMessage());
        response.put("details", e.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
