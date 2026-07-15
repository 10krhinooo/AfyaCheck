//package com.kimanga.afyacheck.service;
//
//import com.kimanga.afyacheck.model.HealthCenter;
//import com.kimanga.afyacheck.repository.HealthCenterRepository;
//import jakarta.annotation.PostConstruct;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Service
//public class HealthCenterService {
//
//    private final HealthCenterRepository repository;
//    private final RestTemplate restTemplate;
//
//    @Value("${google.maps.api.key}")
//    private String apiKey;
//
//    public HealthCenterService(HealthCenterRepository repository, RestTemplate restTemplate) {
//        this.repository = repository;
//        this.restTemplate = restTemplate;
//    }
//
//    public List<HealthCenter> findNearbyHealthCenters(Double lat, Double lng, Double radius, String type) {
//        if (type != null && !type.isEmpty()) {
//            return repository.findNearbyHealthCentersByType(lat, lng, radius, type);
//        }
//        return repository.findNearbyHealthCenters(lat, lng, radius);
//    }
//
//    public List<HealthCenter> findAll() {
//        return repository.findAll();
//    }
//
//    public HealthCenter save(HealthCenter healthCenter) {
//        return repository.save(healthCenter);
//    }
//
//    // Optional: Initialize with sample data if table is empty
//    @PostConstruct
//    public void initSampleData() {
//        if (repository.count() == 0) {
//            List<HealthCenter> centers = Arrays.asList(
//                    new HealthCenter("City General Hospital", "123 Main Street", 40.7128, -74.0060,
//                            "HOSPITAL", "555-0001"),
//                    new HealthCenter("Downtown Medical Clinic", "456 Oak Avenue", 40.7215, -74.0052,
//                            "CLINIC", "555-0002"),
//                    new HealthCenter("24/7 Urgent Care", "789 Pine Street", 40.7150, -74.0090,
//                            "URGENT_CARE", "555-0003"),
//                    new HealthCenter("Community Pharmacy", "321 Elm Street", 40.7080, -74.0080,
//                            "PHARMACY", "555-0004")
//            );
//            repository.saveAll(centers);
//        }
//    }
//}