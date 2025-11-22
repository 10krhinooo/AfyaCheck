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

    private Process pythonProcess;
    private boolean isRunning = false;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (autoStartMLService) {
            startMLService();
        }
    }

    public void startMLService() {
        if (isRunning) {
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

            pythonProcess = processBuilder.start();
            isRunning = true;

            logger.info("ML service started on port 8000");

            // Monitor the process
            monitorPythonProcess();

        } catch (IOException e) {
            logger.error("Failed to start ML service: {}", e.getMessage());
            isRunning = false;
        }
    }

    public void stopMLService() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(5, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                pythonProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isRunning = false;
            logger.info("ML service stopped");
        }
    }

    private void monitorPythonProcess() {
        scheduler.scheduleAtFixedRate(() -> {
            if (pythonProcess != null && !pythonProcess.isAlive()) {
                logger.warn("ML service process died, attempting restart...");
                isRunning = false;
                if (autoStartMLService) {
                    startMLService();
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public boolean isMLServiceRunning() {
        return isRunning && pythonProcess != null && pythonProcess.isAlive();
    }
}