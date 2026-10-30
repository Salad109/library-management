package librarymanagement;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred. Please try again later";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Reshapes Spring's ProblemDetail responses into a custom error format
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        if (statusCode.is5xxServerError()) {
            logUnexpectedError(ex, ((ServletWebRequest) request).getRequest());
            return new ResponseEntity<>(errorBody(UNEXPECTED_ERROR_MESSAGE), headers, statusCode);
        }

        String message = ex.getMessage();
        if (body instanceof ProblemDetail problemDetail) {
            message = problemDetail.getDetail();
        } else if (ex instanceof ErrorResponse errorResponse) {
            message = errorResponse.getBody().getDetail();
        }
        return new ResponseEntity<>(errorBody(message), headers, statusCode);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock() {
        return new ResponseEntity<>(errorBody("This item was just modified by another user. Please try again later."), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return new ResponseEntity<>(errorBody(ex.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedError(Exception ex, HttpServletRequest request) {
        logUnexpectedError(ex, request);
        return new ResponseEntity<>(errorBody(UNEXPECTED_ERROR_MESSAGE), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void logUnexpectedError(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred during request to {} {}", request.getMethod(), request.getRequestURI(), ex);
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
