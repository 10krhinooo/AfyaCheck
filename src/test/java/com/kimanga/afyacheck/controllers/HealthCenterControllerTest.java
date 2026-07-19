package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.model.HealthCenter;
import com.kimanga.afyacheck.service.HealthCenterService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthCenterControllerTest {

    private final HealthCenterService healthCenterService = mock(HealthCenterService.class);
    private final HealthCenterController controller = new HealthCenterController(healthCenterService);

    @Test
    void mapsKeyReturnsConfiguredKey() {
        ReflectionTestUtils.setField(controller, "apiKey", "test-api-key");

        assertThat(controller.mapsKey()).containsEntry("apiKey", "test-api-key");
    }

    @Test
    void nearbyDelegatesToHealthCenterService() {
        HealthCenter center = new HealthCenter();
        when(healthCenterService.findNearby(-1.28, 36.82, 10)).thenReturn(List.of(center));

        List<HealthCenter> result = controller.nearby(-1.28, 36.82);

        assertThat(result).containsExactly(center);
    }
}
