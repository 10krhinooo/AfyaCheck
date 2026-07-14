package com.kimanga.afyacheck.config;

import org.hibernate.AssertionFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.persistence.PersistenceException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AssertionFailure.class)
    public ResponseEntity<Map<String, String>> handleAssertionFailure(AssertionFailure ex) {
        logger.error("Hibernate AssertionFailure: ", ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Database error occurred");
        response.put("message", "Please try again. If the problem persists, contact support.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Map<String, String>> handlePersistenceException(PersistenceException ex) {
        logger.error("Persistence exception: ", ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Database error occurred");
        response.put("message", "There was a problem saving your data. Please try again.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalStateException(IllegalStateException ex) {
        logger.error("Illegal state exception: ", ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Invalid data");
        response.put("message", "The request could not be processed. Please check your input and try again.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception: ", ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Application error");
        response.put("message", "An unexpected error occurred. Please try again. If the problem persists, contact support.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: ", ex);
        Map<String, String> response = new HashMap<>();
        response.put("error", "Unexpected error");
        response.put("message", "An unexpected error occurred. Please try again.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

