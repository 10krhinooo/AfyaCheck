package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.BlacklistedPlace;
import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.repository.BlacklistedPlaceRepository;
import com.kimanga.afyacheck.repository.HealthCenterRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class HealthCenterService {

    private static final double EARTH_RADIUS_KM = 6371;

    private final HealthCenterRepository healthCenterRepository;
    private final BlacklistedPlaceRepository blacklistedPlaceRepository;

    public HealthCenterService(
            HealthCenterRepository healthCenterRepository,
            BlacklistedPlaceRepository blacklistedPlaceRepository) {
        this.healthCenterRepository = healthCenterRepository;
        this.blacklistedPlaceRepository = blacklistedPlaceRepository;
    }

    /**
     * Curated centers within radiusKm of the given point, nearest first. The health-centers
     * page falls back to a live Google Places search when this returns an empty list (small
     * curated dataset, no PostGIS -- Haversine in Java is fine at this scale).
     */
    public List<HealthCenter> findNearby(double lat, double lng, double radiusKm) {
        return healthCenterRepository.findByIsActiveTrue().stream()
                .filter(c -> distanceKm(lat, lng, c.getLatitude(), c.getLongitude()) <= radiusKm)
                .sorted(Comparator.comparingDouble(c -> distanceKm(lat, lng, c.getLatitude(), c.getLongitude())))
                .toList();
    }

    /**
     * Google Places IDs an admin has hidden from the live-search supplement (see
     * useNearbyHealthCenters.ts), so the frontend can filter them out before merging live
     * results with curated ones. Public read (no admin auth) since it's needed by the
     * anonymous health-centers page, same access model as findNearby above.
     */
    public List<String> blacklistedPlaceIds() {
        return blacklistedPlaceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BlacklistedPlace::getPlaceId)
                .toList();
    }

    private double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
