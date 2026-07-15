package com.kimanga.afyacheck.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCenterControllerTest {

    @Test
    void mapsKeyReturnsConfiguredKey() {
        HealthCenterController controller = new HealthCenterController();
        ReflectionTestUtils.setField(controller, "apiKey", "test-api-key");

        assertThat(controller.mapsKey()).containsEntry("apiKey", "test-api-key");
    }
}
