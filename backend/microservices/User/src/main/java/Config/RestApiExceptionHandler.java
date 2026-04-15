package Config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Remplace la page Whitelabel (405, etc.) par du JSON lisible dans le navigateur ou Postman.
 */
@RestControllerAdvice
public class RestApiExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        body.put("error", "Method Not Allowed");
        body.put("path", request.getRequestURI());
        body.put("method", request.getMethod());
        body.put("supportedMethods", ex.getSupportedHttpMethods());
        body.put("hint", "Ex. /api/auth/signin et /api/auth/signup attendent POST ; GET /api/auth/signin affiche l'aide. "
                + "MatchFreelance : POST /api/users/auth/login.");
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
