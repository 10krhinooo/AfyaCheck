package com.kimanga.afyacheck.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JSON API backing the React health centers flow (see /app/health-centers). Converted from
 * server-rendered MVC to REST as part of the Thymeleaf -> React migration (Phase 5). The API
 * key is still visible to the browser either way (unavoidable for client-side Maps JS) — the
 * real control is an HTTP-referrer restriction on the key in Google Cloud Console, not
 * endpoint secrecy.
 */
@RestController
public class HealthCenterController {

    @Value("${google.maps.api.key}")
    private String apiKey;

    @GetMapping("/api/config/maps-key")
    public Map<String, String> mapsKey() {
        return Map.of("apiKey", apiKey);
    }
}
