package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.BlacklistedPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlacklistedPlaceRepository extends JpaRepository<BlacklistedPlace, Long> {

    List<BlacklistedPlace> findAllByOrderByCreatedAtDesc();

    boolean existsByPlaceId(String placeId);

    void deleteByPlaceId(String placeId);
}
