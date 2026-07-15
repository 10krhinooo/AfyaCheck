package com.kimanga.afyacheck.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PythonConfigTest {

    @TempDir
    Path tempDir;

    private PythonConfig configWithScriptPath(String path) {
        PythonConfig config = new PythonConfig();
        ReflectionTestUtils.setField(config, "pythonExecutablePath", "python3");
        ReflectionTestUtils.setField(config, "mlScriptPath", path);
        ReflectionTestUtils.setField(config, "decisionTreeScriptPath", path);
        ReflectionTestUtils.setField(config, "decisionTreeServiceUrl", "http://localhost:8001");
        ReflectionTestUtils.setField(config, "mlServiceUrl", "http://localhost:8000");
        return config;
    }

    @Test
    void gettersReturnConfiguredValues() {
        PythonConfig config = configWithScriptPath("/nonexistent/script.py");
        assertThat(config.getPythonExecutablePath()).isEqualTo("python3");
        assertThat(config.getMlScriptPath()).isEqualTo("/nonexistent/script.py");
        assertThat(config.getDecisionTreeScriptPath()).isEqualTo("/nonexistent/script.py");
        assertThat(config.getDecisionTreeServiceUrl()).isEqualTo("http://localhost:8001");
        assertThat(config.getMlServiceUrl()).isEqualTo("http://localhost:8000");
    }

    @Test
    void isMlServiceConfiguredFalseWhenScriptMissing() {
        PythonConfig config = configWithScriptPath("/definitely/not/here.py");
        assertThat(config.isMlServiceConfigured()).isFalse();
    }

    @Test
    void isMlServiceConfiguredTrueWhenScriptExists() throws Exception {
        File script = tempDir.resolve("app.py").toFile();
        assertThat(script.createNewFile()).isTrue();
        PythonConfig config = configWithScriptPath(script.getAbsolutePath());
        assertThat(config.isMlServiceConfigured()).isTrue();
    }

    @Test
    void isDecisionTreeServiceConfiguredFalseWhenScriptMissing() {
        PythonConfig config = configWithScriptPath("/definitely/not/here.py");
        assertThat(config.isDecisionTreeServiceConfigured()).isFalse();
    }

    @Test
    void getMlServicesStatusUnavailableWhenNothingConfigured() {
        PythonConfig config = configWithScriptPath("/definitely/not/here.py");
        assertThat(config.getMlServicesStatus()).isEqualTo("UNAVAILABLE");
    }

    @Test
    void getMlServicesStatusMlServiceOnlyWhenOnlyMlScriptExists() throws Exception {
        File script = tempDir.resolve("app.py").toFile();
        assertThat(script.createNewFile()).isTrue();

        PythonConfig config = new PythonConfig();
        ReflectionTestUtils.setField(config, "mlScriptPath", script.getAbsolutePath());
        ReflectionTestUtils.setField(config, "decisionTreeScriptPath", "/definitely/not/here.py");

        assertThat(config.getMlServicesStatus()).isEqualTo("ML_SERVICE_ONLY");
    }

    @Test
    void validatePythonEnvironmentRunsWithoutThrowing() {
        PythonConfig config = configWithScriptPath("/definitely/not/here.py");
        config.validatePythonEnvironment();
    }

    @Test
    void validatePythonEnvironmentWarnsOnNonZeroExitCode() {
        // "false" is a real executable on Linux that always exits with status 1,
        // exercising the exitCode != 0 branch (as opposed to a missing executable,
        // which exercises the catch-exception branch already covered above).
        PythonConfig config = configWithScriptPath("/definitely/not/here.py");
        ReflectionTestUtils.setField(config, "pythonExecutablePath", "false");
        config.validatePythonEnvironment();
    }

    @Test
    void isDecisionTreeServiceConfiguredTrueWhenScriptAndModelsExist() throws Exception {
        // The repo's real ./python-service/decision_tree_model directory (relative to
        // the working directory tests run from) already ships real .joblib files, so
        // only the script path needs to exist to hit the "configured" branch.
        File script = tempDir.resolve("decision_tree_service.py").toFile();
        assertThat(script.createNewFile()).isTrue();

        PythonConfig config = new PythonConfig();
        ReflectionTestUtils.setField(config, "decisionTreeScriptPath", script.getAbsolutePath());

        assertThat(config.isDecisionTreeServiceConfigured()).isTrue();
    }

    @Test
    void getMlServicesStatusFullyOperationalWhenBothConfigured() throws Exception {
        File mlScript = tempDir.resolve("app.py").toFile();
        assertThat(mlScript.createNewFile()).isTrue();
        File dtScript = tempDir.resolve("decision_tree_service.py").toFile();
        assertThat(dtScript.createNewFile()).isTrue();

        PythonConfig config = new PythonConfig();
        ReflectionTestUtils.setField(config, "mlScriptPath", mlScript.getAbsolutePath());
        ReflectionTestUtils.setField(config, "decisionTreeScriptPath", dtScript.getAbsolutePath());

        assertThat(config.getMlServicesStatus()).isEqualTo("FULLY_OPERATIONAL");
    }

    @Test
    void getMlServicesStatusDecisionTreeOnlyWhenOnlyDecisionTreeScriptExists() throws Exception {
        File dtScript = tempDir.resolve("decision_tree_service.py").toFile();
        assertThat(dtScript.createNewFile()).isTrue();

        PythonConfig config = new PythonConfig();
        ReflectionTestUtils.setField(config, "mlScriptPath", "/definitely/not/here.py");
        ReflectionTestUtils.setField(config, "decisionTreeScriptPath", dtScript.getAbsolutePath());

        assertThat(config.getMlServicesStatus()).isEqualTo("DECISION_TREE_ONLY");
    }
}
