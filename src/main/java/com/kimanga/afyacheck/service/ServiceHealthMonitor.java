package com.kimanga.afyacheck.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Caches ML/decision-tree health status for a short TTL so the admin
 * dashboard doesn't pay a full RestTemplate timeout on every page load
 * when a Python service is down.
 */
@Component
public class ServiceHealthMonitor {

    private static final long TTL_MILLIS = 30_000;

    private final MLService mlService;
    private final DecisionTreeClient decisionTreeClient;

    private final AtomicReference<Map<String, Boolean>> cached = new AtomicReference<>();
    private final AtomicLong cachedAt = new AtomicLong(0);

    public ServiceHealthMonitor(MLService mlService, DecisionTreeClient decisionTreeClient) {
        this.mlService = mlService;
        this.decisionTreeClient = decisionTreeClient;
    }

    public Map<String, Boolean> getHealth() {
        long now = System.currentTimeMillis();
        Map<String, Boolean> current = cached.get();

        if (current != null && (now - cachedAt.get()) < TTL_MILLIS) {
            return current;
        }

        Map<String, Boolean> fresh = Map.of(
                "mlServiceHealthy", mlService.isServiceHealthy(),
                "decisionTreeHealthy", decisionTreeClient.isServiceHealthy()
        );
        cached.set(fresh);
        cachedAt.set(now);
        return fresh;
    }
}
