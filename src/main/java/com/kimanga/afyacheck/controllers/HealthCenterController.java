package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.service.HealthCenterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    private static final double DEFAULT_RADIUS_KM = 10;

    private final HealthCenterService healthCenterService;

    @Value("${google.maps.api.key}")
    private String apiKey;

    public HealthCenterController(HealthCenterService healthCenterService) {
        this.healthCenterService = healthCenterService;
    }

    @GetMapping("/api/config/maps-key")
    public Map<String, String> mapsKey() {
        return Map.of("apiKey", apiKey);
    }

    // Curated centers near the given point; the frontend merges these with a live Google
    // Places search (see useNearbyHealthCenters.ts).
    @GetMapping("/api/health-centers/nearby")
    public List<HealthCenter> nearby(@RequestParam double lat, @RequestParam double lng) {
        return healthCenterService.findNearby(lat, lng, DEFAULT_RADIUS_KM);
    }

    // Google place_ids an admin has hidden from the live-search supplement; the frontend
    // filters these out of the Places results before merging (see AdminController's
    // /admin/health-centers/blacklist for how entries get added).
    @GetMapping("/api/health-centers/blacklisted-place-ids")
    public List<String> blacklistedPlaceIds() {
        return healthCenterService.blacklistedPlaceIds();
    }
}
