package tn.esprit.evaluation.exception;

/**
 * Exception levée lorsqu'une ressource (examen, certificat, passage) est introuvable.
 * Le GlobalExceptionHandler renvoie 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " non trouvé: " + id);
    }
}
