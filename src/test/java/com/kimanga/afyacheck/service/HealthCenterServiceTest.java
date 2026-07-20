package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.BlacklistedPlace;
import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.repository.BlacklistedPlaceRepository;
import com.kimanga.afyacheck.repository.HealthCenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthCenterServiceTest {

    private HealthCenterRepository healthCenterRepository;
    private BlacklistedPlaceRepository blacklistedPlaceRepository;
    private HealthCenterService healthCenterService;

    @BeforeEach
    void setUp() {
        healthCenterRepository = mock(HealthCenterRepository.class);
        blacklistedPlaceRepository = mock(BlacklistedPlaceRepository.class);
        healthCenterService = new HealthCenterService(healthCenterRepository, blacklistedPlaceRepository);
    }

    private HealthCenter center(String name, double lat, double lng) {
        HealthCenter center = new HealthCenter();
        center.setName(name);
        center.setLatitude(lat);
        center.setLongitude(lng);
        return center;
    }

    @Test
    void findNearbyReturnsOnlyCentersWithinRadiusSortedByDistance() {
        // Nairobi CBD as the search point (-1.2864, 36.8172).
        HealthCenter near = center("Near", -1.2864, 36.8172);
        HealthCenter far = center("Far", 1.0, 40.0);
        when(healthCenterRepository.findByIsActiveTrue()).thenReturn(List.of(far, near));

        List<HealthCenter> result = healthCenterService.findNearby(-1.2864, 36.8172, 10);

        assertThat(result).containsExactly(near);
    }

    @Test
    void findNearbyReturnsEmptyWhenNoActiveCenters() {
        when(healthCenterRepository.findByIsActiveTrue()).thenReturn(List.of());

        List<HealthCenter> result = healthCenterService.findNearby(-1.2864, 36.8172, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void blacklistedPlaceIdsMapsRepositoryEntriesToPlaceIds() {
        BlacklistedPlace first = new BlacklistedPlace();
        first.setPlaceId("place-1");
        BlacklistedPlace second = new BlacklistedPlace();
        second.setPlaceId("place-2");
        when(blacklistedPlaceRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(first, second));

        List<String> result = healthCenterService.blacklistedPlaceIds();

        assertThat(result).containsExactly("place-1", "place-2");
    }
}
