package com.freelancing.contract.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Réponses 400 avec corps JSON {@code error}, pour que Feign et les clients voient la cause
 * (sinon corps vide et message Feign peu parlant).
 */
@RestControllerAdvice
public class ContractExceptionHandler {

    private static final String JSON_ERROR_KEY = "error";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String msg =
                e.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(Map.of(JSON_ERROR_KEY, msg.isEmpty() ? "Validation failed" : msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        String msg = e.getMostSpecificCause().getMessage();
        return ResponseEntity.badRequest()
                .body(Map.of(JSON_ERROR_KEY, msg != null ? msg : "Invalid request body"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException e) {
        String msg = e.getMostSpecificCause().getMessage();
        return ResponseEntity.badRequest()
                .body(
                        Map.of(
                                JSON_ERROR_KEY,
                                msg != null
                                        ? msg
                                        : "Data integrity violation (ex. contrainte unique en base)"));
    }
}
