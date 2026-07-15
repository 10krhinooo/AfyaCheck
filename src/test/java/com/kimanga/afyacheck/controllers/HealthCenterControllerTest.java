package com.kimanga.afyacheck.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCenterControllerTest {

    @Test
    void healthCentersPageAddsApiKeyToModel() {
        HealthCenterController controller = new HealthCenterController();
        ReflectionTestUtils.setField(controller, "apiKey", "test-api-key");
        Model model = new ExtendedModelMap();

        String view = controller.healthCentersPage(model);

        assertThat(view).isEqualTo("health-centers");
        assertThat(model.getAttribute("apiKey")).isEqualTo("test-api-key");
    }

    @Test
    void healthCentersPageHandlesNullApiKey() {
        HealthCenterController controller = new HealthCenterController();
        ReflectionTestUtils.setField(controller, "apiKey", null);
        Model model = new ExtendedModelMap();

        String view = controller.healthCentersPage(model);

        assertThat(view).isEqualTo("health-centers");
        assertThat(model.getAttribute("apiKey")).isNull();
    }
}
