package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.repository.HealthCenterRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class HealthCenterService {

    private static final double EARTH_RADIUS_KM = 6371;

    private final HealthCenterRepository healthCenterRepository;

    public HealthCenterService(HealthCenterRepository healthCenterRepository) {
        this.healthCenterRepository = healthCenterRepository;
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

    private double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
