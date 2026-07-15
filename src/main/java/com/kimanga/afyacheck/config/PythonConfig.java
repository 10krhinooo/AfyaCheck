package com.kimanga.afyacheck.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import java.io.File;

@Configuration
public class PythonConfig {

    private static final Logger logger = LoggerFactory.getLogger(PythonConfig.class);

    @Value("${python.executable.path:python}")
    private String pythonExecutablePath;

    @Value("${ml.service.script.path:./ml-service/app.py}")
    private String mlScriptPath;

    @Value("${decision.tree.service.script.path:./python-service/decision_tree_service.py}")
    private String decisionTreeScriptPath;

    @Value("${decision.tree.service.url:http://localhost:8001}")
    private String decisionTreeServiceUrl;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @EventListener(ContextRefreshedEvent.class)
    public void validatePythonEnvironment() {
        validatePythonInstallation();
        validateMLServiceScript();
        validateDecisionTreeServiceScript();
        logServiceConfiguration();
    }

    private void validatePythonInstallation() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutablePath, "--version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("✅ Python environment is properly configured");
            } else {
                logger.warn("⚠️ Python may not be properly configured");
            }

        } catch (Exception e) {
            logger.error("❌ Error validating Python environment: {}", e.getMessage());
        }
    }

    private void validateMLServiceScript() {
        File scriptFile = new File(mlScriptPath);
        if (scriptFile.exists()) {
            logger.info("✅ ML service script found at: {}", mlScriptPath);
        } else {
            logger.warn("⚠️ ML service script not found at: {}", mlScriptPath);
            logger.info("💡 Expected ML service for risk prediction at: {}", mlScriptPath);
        }
    }

    private void validateDecisionTreeServiceScript() {
        File scriptFile = new File(decisionTreeScriptPath);
        if (scriptFile.exists()) {
            logger.info("✅ Decision Tree service script found at: {}", decisionTreeScriptPath);
        } else {
            logger.warn("⚠️ Decision Tree service script not found at: {}", decisionTreeScriptPath);
            logger.info("💡 Expected Decision Tree service for question sequencing at: {}", decisionTreeScriptPath);
        }
    }

    private void logServiceConfiguration() {
        logger.info("🎯 ML Services Configuration:");
        logger.info("   ├── Python Executable: {}", pythonExecutablePath);
        logger.info("   ├── ML Service (Risk Prediction):");
        logger.info("   │   ├── Script: {}", mlScriptPath);
        logger.info("   │   └── URL: {}", mlServiceUrl);
        logger.info("   └── Decision Tree Service (Question Sequencing):");
        logger.info("       ├── Script: {}", decisionTreeScriptPath);
        logger.info("       └── URL: {}", decisionTreeServiceUrl);

        // Check model directory
        File modelDir = new File("./python-service/decision_tree_model");
        if (modelDir.exists()) {
            File[] modelFiles = modelDir.listFiles((dir, name) -> name.endsWith(".joblib"));
            if (modelFiles != null && modelFiles.length > 0) {
                logger.info("✅ Decision Tree models found: {} files", modelFiles.length);
                for (File modelFile : modelFiles) {
                    logger.info("   ├── Model: {} ({} bytes)",
                            modelFile.getName(), modelFile.length());
                }
            } else {
                logger.warn("⚠️ No .joblib model files found in: {}", modelDir.getAbsolutePath());
            }
        } else {
            logger.warn("⚠️ Decision Tree models directory not found: {}", modelDir.getAbsolutePath());
        }
    }

    // Getters
    public String getPythonExecutablePath() {
        return pythonExecutablePath;
    }

    public String getMlScriptPath() {
        return mlScriptPath;
    }

    public String getDecisionTreeScriptPath() {
        return decisionTreeScriptPath;
    }

    public String getDecisionTreeServiceUrl() {
        return decisionTreeServiceUrl;
    }

    public String getMlServiceUrl() {
        return mlServiceUrl;
    }

    /**
     * Check if decision tree service is expected to be available
     */
    public boolean isDecisionTreeServiceConfigured() {
        File scriptFile = new File(decisionTreeScriptPath);
        File modelDir = new File("./python-service/decision_tree_model");
        File[] modelFiles = modelDir.listFiles((dir, name) -> name.endsWith(".joblib"));

        return scriptFile.exists() && modelFiles != null && modelFiles.length > 0;
    }

    /**
     * Check if ML service is expected to be available
     */
    public boolean isMlServiceConfigured() {
        File scriptFile = new File(mlScriptPath);
        return scriptFile.exists();
    }

    /**
     * Get overall ML services status
     */
    public String getMlServicesStatus() {
        boolean mlServiceReady = isMlServiceConfigured();
        boolean decisionTreeServiceReady = isDecisionTreeServiceConfigured();

        if (mlServiceReady && decisionTreeServiceReady) {
            return "FULLY_OPERATIONAL";
        } else if (mlServiceReady) {
            return "ML_SERVICE_ONLY";
        } else if (decisionTreeServiceReady) {
            return "DECISION_TREE_ONLY";
        } else {
            return "UNAVAILABLE";
        }
    }
}