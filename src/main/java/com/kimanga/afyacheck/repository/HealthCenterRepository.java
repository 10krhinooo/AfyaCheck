//package com.kimanga.afyacheck.repository;
//
//import com.kimanga.afyacheck.model.HealthCenter;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//
//public interface HealthCenterRepository extends JpaRepository<HealthCenter,Long> {
//    // Haversine formula for distance calculation in kilometers
//    @Query(value = "SELECT * FROM health_centers h WHERE " +
//            "(6371 * acos(cos(radians(:lat)) * cos(radians(h.latitude)) * " +
//            "cos(radians(h.longitude) - radians(:lng)) + " +
//            "sin(radians(:lat)) * sin(radians(h.latitude)))) < :radius",
//            nativeQuery = true)
//    List<HealthCenter> findNearbyHealthCenters(@Param("lat") Double latitude,
//                                               @Param("lng") Double longitude,
//                                               @Param("radius") Double radius);
//
//    List<HealthCenter> findByType(String type);
//    List<HealthCenter> findByEmergencyServicesTrue();
//
//    @Query(value = "SELECT * FROM health_centers h WHERE " +
//            "(6371 * acos(cos(radians(:lat)) * cos(radians(h.latitude)) * " +
//            "cos(radians(h.longitude) - radians(:lng)) + " +
//            "sin(radians(:lat)) * sin(radians(h.latitude)))) < :radius " +
//            "AND h.type = :type",
//            nativeQuery = true)
//    List<HealthCenter> findNearbyHealthCentersByType(@Param("lat") Double latitude,
//                                                     @Param("lng") Double longitude,
//                                                     @Param("radius") Double radius,
//                                                     @Param("type") String type);
//}
