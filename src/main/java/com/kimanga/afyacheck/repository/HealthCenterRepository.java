package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.HealthCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthCenterRepository extends JpaRepository<HealthCenter, Long> {

    List<HealthCenter> findByIsActiveTrue();

    Long countByIsActiveTrue();
}
