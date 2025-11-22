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

    @EventListener(ContextRefreshedEvent.class)
    public void validatePythonEnvironment() {
        try {
            // Check if Python is available
            ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutablePath, "--version");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info("Python environment is properly configured");
            } else {
                logger.warn("Python may not be properly configured");
            }

            // Check if ML service script exists
            File scriptFile = new File(mlScriptPath);
            if (scriptFile.exists()) {
                logger.info("ML service script found at: {}", mlScriptPath);
            } else {
                logger.warn("ML service script not found at: {}", mlScriptPath);
            }

        } catch (Exception e) {
            logger.error("Error validating Python environment: {}", e.getMessage());
        }
    }

    public String getPythonExecutablePath() {
        return pythonExecutablePath;
    }

    public String getMlScriptPath() {
        return mlScriptPath;
    }
}