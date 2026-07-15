package com.kimanga.afyacheck.config;

import jakarta.persistence.PersistenceException;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesAssertionFailure() {
        ResponseEntity<Map<String, String>> response =
                handler.handleAssertionFailure(new AssertionFailure("boom"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "Database error occurred");
    }

    @Test
    void handlesPersistenceException() {
        ResponseEntity<Map<String, String>> response =
                handler.handlePersistenceException(new PersistenceException("boom"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "Database error occurred");
    }

    @Test
    void handlesIllegalStateException() {
        ResponseEntity<Map<String, String>> response =
                handler.handleIllegalStateException(new IllegalStateException("bad state"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "bad state");
    }

    @Test
    void handlesRuntimeException() {
        ResponseEntity<Map<String, String>> response =
                handler.handleRuntimeException(new RuntimeException("oops"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "oops");
    }

    @Test
    void handlesGenericException() {
        ResponseEntity<Map<String, String>> response =
                handler.handleGenericException(new Exception("unexpected"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("error", "Unexpected error");
    }
}
