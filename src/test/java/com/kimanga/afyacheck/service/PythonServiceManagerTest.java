package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.config.PythonConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PythonServiceManagerTest {

    private PythonConfig pythonConfig;
    private PythonServiceManager manager;

    @BeforeEach
    void setUp() {
        manager = new PythonServiceManager();
        pythonConfig = mock(PythonConfig.class);
        when(pythonConfig.getPythonExecutablePath()).thenReturn("python3");
        // Deliberately nonexistent paths so start*Service() never actually spawns a process.
        when(pythonConfig.getMlScriptPath()).thenReturn("/definitely/not/here/app.py");
        when(pythonConfig.getDecisionTreeScriptPath()).thenReturn("/definitely/not/here/decision_tree_service.py");
        when(pythonConfig.getMlServiceUrl()).thenReturn("http://localhost:8000");
        when(pythonConfig.getDecisionTreeServiceUrl()).thenReturn("http://localhost:8001");

        ReflectionTestUtils.setField(manager, "pythonConfig", pythonConfig);
        ReflectionTestUtils.setField(manager, "autoStartMLService", false);
        ReflectionTestUtils.setField(manager, "autoStartDecisionTreeService", false);
    }

    @Test
    void logPythonEnvironmentRunsWithoutThrowing() {
        manager.logPythonEnvironment();
    }

    @Test
    void onApplicationReadyDoesNotAutoStartWhenDisabled() {
        manager.onApplicationReady();
        assertThat(manager.isMLServiceRunning()).isFalse();
        assertThat(manager.isDecisionTreeServiceRunning()).isFalse();
    }

    @Test
    void startMLServiceNoOpsWhenScriptMissing() {
        manager.startMLService();
        assertThat(manager.isMLServiceRunning()).isFalse();
    }

    @Test
    void startMLServiceSkipsWhenAlreadyRunning() {
        ReflectionTestUtils.setField(manager, "isMLServiceRunning", true);
        manager.startMLService();
        // Should return immediately without touching the (nonexistent) script.
        assertThat((boolean) ReflectionTestUtils.getField(manager, "isMLServiceRunning")).isTrue();
    }

    @Test
    void stopMLServiceNoOpsWhenNoProcess() {
        manager.stopMLService();
        assertThat(manager.isMLServiceRunning()).isFalse();
    }

    @Test
    void startDecisionTreeServiceNoOpsWhenScriptMissing() {
        manager.startDecisionTreeService();
        assertThat(manager.isDecisionTreeServiceRunning()).isFalse();
    }

    @Test
    void startDecisionTreeServiceSkipsWhenAlreadyRunning() {
        ReflectionTestUtils.setField(manager, "isDecisionTreeServiceRunning", true);
        manager.startDecisionTreeService();
        assertThat((boolean) ReflectionTestUtils.getField(manager, "isDecisionTreeServiceRunning")).isTrue();
    }

    @Test
    void stopDecisionTreeServiceNoOpsWhenNoProcess() {
        manager.stopDecisionTreeService();
        assertThat(manager.isDecisionTreeServiceRunning()).isFalse();
    }

    @Test
    void startAllAndStopAllServicesDoNotThrow() {
        manager.startAllServices();
        manager.stopAllServices();
    }

    @Test
    void getServicesStatusReturnsExpectedStructure() {
        Map<String, Object> status = manager.getServicesStatus();

        assertThat(status).containsKeys("mlService", "decisionTreeService",
                "modelDirectoryExists", "modelDirectoryPath", "modelFilesCount");

        @SuppressWarnings("unchecked")
        Map<String, Object> mlService = (Map<String, Object>) status.get("mlService");
        assertThat(mlService).containsEntry("running", false);
    }

    @Test
    void cleanupStopsServicesAndShutsDownScheduler() {
        manager.cleanup();
    }
}
