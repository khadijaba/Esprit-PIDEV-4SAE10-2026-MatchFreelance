package esprit.project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Évite un HTTP 500 opaque quand la colonne {@code status} (ENUM) refuse la valeur DRAFT.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String msg = cause != null ? cause.getMessage() : ex.getMessage();
        log.warn("DataIntegrityViolation: {}", msg);

        String detail = msg != null ? msg : "Violation de contrainte en base.";
        if (msg != null && msg.toLowerCase().contains("status")) {
            detail += " Si vous utilisez MySQL avec un ENUM sur status, exécutez : "
                    + "ALTER TABLE project MODIFY COLUMN status VARCHAR(32) NOT NULL;";
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Contrainte base de données");
        pd.setType(URI.create("about:blank"));
        return pd;
    }
}
