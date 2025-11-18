package com.rybka.ticketing.handlers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.databind.util.ClassUtil.getRootCause;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex){
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 400);
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .toList();
        body.put("errors", details);
        return body;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,Object>> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                  HttpServletRequest req) {
        Throwable root = getRootCause(ex);

        String constraint = null;
        String sqlState = null;

        if (root instanceof org.hibernate.exception.ConstraintViolationException cve) {
            constraint = cve.getConstraintName();
            if (cve.getSQLException() != null) {
                sqlState = cve.getSQLException().getSQLState();
            }
        } else if (root instanceof java.sql.SQLException se) {
            sqlState = se.getSQLState();
        }

        String message = "Data integrity violation";
        HttpStatus status = HttpStatus.CONFLICT; // 409 domyślnie

        if ("users_email_key".equalsIgnoreCase(constraint)) {
            message = "email_already_exists";
        } else if ("reservation_hold_idempotency_uk".equalsIgnoreCase(constraint)) {
            message = "idempotency_key_already_used";
        }

        else if ("event_seats_status_check".equalsIgnoreCase(constraint)) {
            message = "invalid_event_seat_status";
        }

        else if (sqlState != null && sqlState.equals("23503")) {
            message = "foreign_key_violation";
        }

        else if (sqlState != null && sqlState.equals("23502")) {
            message = "not_null_violation";
        }

        Map<String,Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", req.getRequestURI());

        return new ResponseEntity<>(body, status);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> handleRSE(ResponseStatusException ex, HttpServletRequest req) {
        Map<String,Object> body = new LinkedHashMap<>();
        int code = ex.getStatusCode().value();
        body.put("status", code);
        body.put("error", ex.getStatusCode().toString().toLowerCase()); // np. "unauthorized"
        body.put("message", ex.getReason());
        body.put("path", req.getRequestURI());
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

}
