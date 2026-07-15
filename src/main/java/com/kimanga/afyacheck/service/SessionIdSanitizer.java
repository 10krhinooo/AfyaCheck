package com.kimanga.afyacheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Normalizes raw HTTP session IDs into the safe, bounded-length IDs that
 * Session rows are keyed by. Split out of SessionService since callers
 * historically pass IDs contaminated with duplicated/comma-joined values
 * (an upstream artifact, not something SessionService itself produces),
 * and resolving that is a distinct concern from session persistence.
 */
@Component
public class SessionIdSanitizer {

    private static final Logger logger = LoggerFactory.getLogger(SessionIdSanitizer.class);

    // Use a shorter session ID if the original is too problematic
    private static final int MAX_SESSION_ID_LENGTH = 500;

    /**
     * Creates a safe session ID by hashing long session IDs.
     *
     * @param sessionExists callback used to pick the valid part out of a
     *                      comma-joined ID (see {@link #clean}); avoids a
     *                      circular dependency on SessionService.
     */
    public String createSafeSessionId(String originalSessionId, Predicate<String> sessionExists) {
        if (originalSessionId == null) {
            return UUID.randomUUID().toString();
        }

        String cleanSessionId = clean(originalSessionId, sessionExists);

        if (cleanSessionId.length() <= MAX_SESSION_ID_LENGTH) {
            return cleanSessionId;
        }

        String hashBasedId = "sess_" + Integer.toHexString(cleanSessionId.hashCode()) +
                "_" + System.currentTimeMillis();
        logger.warn("Session ID too long ({} chars), using hash-based ID: {}",
                cleanSessionId.length(), hashBasedId);
        return hashBasedId;
    }

    /**
     * Clean session ID by removing duplicates and extra commas.
     */
    public String clean(String sessionId, Predicate<String> sessionExists) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return sessionId;
        }

        String trimmed = sessionId.trim();

        if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            for (String part : parts) {
                String cleanPart = part.trim();
                if (sessionExists.test(cleanPart)) {
                    logger.info("Using valid session ID part: {}", cleanPart);
                    return cleanPart;
                }
            }
            String cleanId = parts[0].trim();
            logger.warn("No valid session ID found, using first part: {}", cleanId);
            return cleanId;
        }

        return trimmed;
    }
}
