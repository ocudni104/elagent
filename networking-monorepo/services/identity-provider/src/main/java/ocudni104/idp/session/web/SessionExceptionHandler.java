package ocudni104.idp.session.web;


import ocudni104.idp.session.domain.exception.SessionExpiredException;
import ocudni104.idp.session.domain.exception.SessionNotFoundException;
import ocudni104.idp.session.domain.exception.SessionRevokedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class SessionExceptionHandler {

    @ExceptionHandler(SessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(SessionNotFoundException ex) {
        return new ErrorResponse("SESSION_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(SessionExpiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleExpired(SessionExpiredException ex) {
        return new ErrorResponse("SESSION_EXPIRED", ex.getMessage());
    }

    @ExceptionHandler(SessionRevokedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleRevoked(SessionRevokedException ex) {
        return new ErrorResponse("SESSION_REVOKED", ex.getMessage());
    }

    public record ErrorResponse(String code, String message) {}
}