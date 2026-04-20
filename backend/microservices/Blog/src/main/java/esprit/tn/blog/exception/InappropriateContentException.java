package esprit.tn.blog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InappropriateContentException extends RuntimeException {
    public InappropriateContentException(String message) {
        super(message);
    }
}
