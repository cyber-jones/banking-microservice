package com.banking.account.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — centralized error handling for all controllers.
 *
 * TEACHING POINT — @RestControllerAdvice:
 * Instead of try/catch in every controller, we define exception handlers ONCE here.
 * Spring automatically routes exceptions to the appropriate @ExceptionHandler method.
 *
 * ProblemDetail (RFC 7807) — the modern standard for API error responses:
 * {
 *   "type": "...",
 *   "title": "Bad Request",
 *   "status": 400,
 *   "detail": "Validation failed",
 *   "instance": "/api/v1/accounts"
 * }
 *
 * TEACHING POINT — HTTP Status Codes:
 *   400 Bad Request        — invalid input (validation failure)
 *   401 Unauthorized       — not authenticated (missing/invalid token)
 *   403 Forbidden          — authenticated but not authorised (wrong role)
 *   404 Not Found          — resource doesn't exist
 *   409 Conflict           — resource already exists
 *   500 Internal Server Error — unexpected server error
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle Bean Validation failures (@Valid annotation).
     * Returns a map of fieldName → error message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Failed");
        pd.setDetail("One or more fields are invalid");
        pd.setProperty("errors", errors);
        return pd;
    }

    /**
     * Handle resource-not-found or illegal argument errors.
     */
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntimeException(RuntimeException ex) {
        log.warn("Runtime exception: {}", ex.getMessage());
        String message = ex.getMessage() != null ? ex.getMessage() : "An error occurred";

        if (message.contains("not found")) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
            pd.setTitle("Not Found");
            pd.setDetail(message);
            return pd;
        }
        if (message.contains("already exists") || message.contains("already taken")) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
            pd.setTitle("Conflict");
            pd.setDetail(message);
            return pd;
        }

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setDetail(message);
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Forbidden");
        pd.setDetail("You do not have permission to perform this action");
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Unauthorized");
        pd.setDetail("Invalid credentials");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Server Error");
        pd.setDetail("An unexpected error occurred");
        return pd;
    }
}
