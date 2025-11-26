package com.kimanga.afyacheck.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import com.kimanga.afyacheck.config.PythonConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class PythonServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(PythonServiceManager.class);

    @Autowired
    private PythonConfig pythonConfig;

    @Value("${ml.service.auto.start:false}")
    private boolean autoStartMLService;

    @Value("${decision.tree.service.auto.start:false}")
    private boolean autoStartDecisionTreeService;

    private Process mlServiceProcess;
    private Process decisionTreeProcess;
    private boolean isMLServiceRunning = false;
    private boolean isDecisionTreeServiceRunning = false;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logPythonEnvironment();

        if (autoStartMLService) {
            startMLService();
        }

        if (autoStartDecisionTreeService) {
            startDecisionTreeService();
        }
    }

    public void logPythonEnvironment() {
        logger.info("Python Services Configuration:");
        logger.info("   Python Executable: {}", pythonConfig.getPythonExecutablePath());
        logger.info("   ML Service (XGBoost - Risk Prediction):");
        logger.info("      Script: {}", pythonConfig.getMlScriptPath());
        logger.info("      URL: {}", pythonConfig.getMlServiceUrl());
        logger.info("      Auto-start: {}", autoStartMLService);
        logger.info("   Decision Tree Service (Question Sequencing):");
        logger.info("      Script: {}", pythonConfig.getDecisionTreeScriptPath());
        logger.info("      URL: {}", pythonConfig.getDecisionTreeServiceUrl());
        logger.info("      Auto-start: {}", autoStartDecisionTreeService);
    }

    // ML Service (XGBoost - Risk Prediction)
    public void startMLService() {
        if (isMLServiceRunning) {
            logger.info("ML service is already running");
            return;
        }

        try {
            String pythonExecutable = pythonConfig.getPythonExecutablePath();
            String scriptPath = pythonConfig.getMlScriptPath();

            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                logger.error("ML service script not found: {}", scriptPath);
                return;
            }

            // For FastAPI with uvicorn
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable, "-m", "uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"
            );
            processBuilder.directory(new File("./ml-service"));
            processBuilder.redirectErrorStream(true);

            mlServiceProcess = processBuilder.start();
            isMLServiceRunning = true;

            logger.info("ML service (XGBoost) started on port 8000");

            // Monitor the process
            monitorMLServiceProcess();

        } catch (IOException e) {
            logger.error("Failed to start ML service: {}", e.getMessage());
            isMLServiceRunning = false;
        }
    }

    public void stopMLService() {
        if (mlServiceProcess != null && mlServiceProcess.isAlive()) {
            mlServiceProcess.destroy();
            try {
                if (!mlServiceProcess.waitFor(5, TimeUnit.SECONDS)) {
                    mlServiceProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                mlServiceProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isMLServiceRunning = false;
            logger.info("ML service stopped");
        }
    }

    private void monitorMLServiceProcess() {
        scheduler.scheduleAtFixedRate(() -> {
            if (mlServiceProcess != null && !mlServiceProcess.isAlive()) {
                logger.warn("ML service process died, attempting restart...");
                isMLServiceRunning = false;
                if (autoStartMLService) {
                    startMLService();
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    // Decision Tree Service (Question Sequencing)
    public void startDecisionTreeService() {
        if (isDecisionTreeServiceRunning) {
            logger.info("Decision Tree service is already running");
            return;
        }

        try {
            String pythonExecutable = pythonConfig.getPythonExecutablePath();
            String scriptPath = pythonConfig.getDecisionTreeScriptPath();

            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                logger.error("Decision Tree service script not found: {}", scriptPath);
                return;
            }

            // Check if model files exist - UPDATED PATH
            File modelDir = new File("./python-service/decision_tree_model");
            if (!modelDir.exists()) {
                logger.error("Decision Tree models directory not found: {}", modelDir.getAbsolutePath());
                return;
            }

            File[] modelFiles = modelDir.listFiles((dir, name) -> name.endsWith(".joblib"));
            if (modelFiles == null || modelFiles.length == 0) {
                logger.error("No .joblib model files found in: {}", modelDir.getAbsolutePath());
                return;
            }

            logger.info("Found {} decision tree model files", modelFiles.length);

            // Start the Decision Tree service
            ProcessBuilder processBuilder = new ProcessBuilder(
                    pythonExecutable, "decision_tree_service.py"
            );
            processBuilder.directory(new File("./python-service"));
            processBuilder.redirectErrorStream(true);

            // Add environment variables if needed
            Map<String, String> environment = processBuilder.environment();
            environment.put("PYTHONPATH", "./python-service");

            decisionTreeProcess = processBuilder.start();
            isDecisionTreeServiceRunning = true;

            logger.info("Decision Tree service started on port 8001");
            logger.info("   Script: {}", scriptPath);
            logger.info("   Models: {} model files", modelFiles.length);
            logger.info("   URL: {}", pythonConfig.getDecisionTreeServiceUrl());

            // Monitor the process
            monitorDecisionTreeProcess();

        } catch (IOException e) {
            logger.error("Failed to start Decision Tree service: {}", e.getMessage());
            isDecisionTreeServiceRunning = false;
        }
    }

    public void stopDecisionTreeService() {
        if (decisionTreeProcess != null && decisionTreeProcess.isAlive()) {
            decisionTreeProcess.destroy();
            try {
                if (!decisionTreeProcess.waitFor(5, TimeUnit.SECONDS)) {
                    decisionTreeProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                decisionTreeProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isDecisionTreeServiceRunning = false;
            logger.info("Decision Tree service stopped");
        }
    }

    private void monitorDecisionTreeProcess() {
        scheduler.scheduleAtFixedRate(() -> {
            if (decisionTreeProcess != null && !decisionTreeProcess.isAlive()) {
                logger.warn("Decision Tree service process died, attempting restart...");
                isDecisionTreeServiceRunning = false;
                if (autoStartDecisionTreeService) {
                    startDecisionTreeService();
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    // Combined service management
    public void startAllServices() {
        startMLService();
        startDecisionTreeService();
    }

    public void stopAllServices() {
        stopMLService();
        stopDecisionTreeService();
    }

    // Status methods
    public boolean isMLServiceRunning() {
        return isMLServiceRunning && mlServiceProcess != null && mlServiceProcess.isAlive();
    }

    public boolean isDecisionTreeServiceRunning() {
        return isDecisionTreeServiceRunning && decisionTreeProcess != null && decisionTreeProcess.isAlive();
    }

    public Map<String, Object> getServicesStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("mlService", Map.of(
                "running", isMLServiceRunning(),
                "autoStart", autoStartMLService,
                "url", pythonConfig.getMlServiceUrl(),
                "scriptPath", pythonConfig.getMlScriptPath()
        ));

        status.put("decisionTreeService", Map.of(
                "running", isDecisionTreeServiceRunning(),
                "autoStart", autoStartDecisionTreeService,
                "url", pythonConfig.getDecisionTreeServiceUrl(),
                "scriptPath", pythonConfig.getDecisionTreeScriptPath()
        ));

        // Check model files for decision tree - UPDATED PATH
        File modelDir = new File("./python-service/decision_tree_model");
        boolean modelDirExists = modelDir.exists();
        status.put("modelDirectoryExists", modelDirExists);
        status.put("modelDirectoryPath", modelDir.getAbsolutePath());

        if (modelDirExists) {
            File[] modelFiles = modelDir.listFiles((dir, name) -> name.endsWith(".joblib"));
            status.put("modelFilesCount", modelFiles != null ? modelFiles.length : 0);
        } else {
            status.put("modelFilesCount", 0);
        }

        return status;
    }

    // Manual control endpoints
    public void restartMLService() {
        stopMLService();
        try {
            Thread.sleep(2000); // Wait 2 seconds before restart
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startMLService();
    }

    public void restartDecisionTreeService() {
        stopDecisionTreeService();
        try {
            Thread.sleep(2000); // Wait 2 seconds before restart
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startDecisionTreeService();
    }

    // Cleanup on application shutdown
    public void cleanup() {
        stopAllServices();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}