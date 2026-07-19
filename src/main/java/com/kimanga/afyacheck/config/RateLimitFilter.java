package com.kimanga.afyacheck.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, per-client-IP rate limiting for the anonymous public API. The questionnaire is
 * deliberately unauthenticated (anonymous screening is a product requirement), which leaves the
 * public endpoints open to abuse; POST /api/results/notify in particular sends an email to an
 * arbitrary address, so without a limit it's a spam relay. Fixed-window counters (no external
 * dependency, no shared store) are enough for a single-instance deployment; a multi-instance
 * deployment would need to move this to a shared store or the ingress layer.
 *
 * Client identity is request.getRemoteAddr(). Behind a reverse proxy set
 * server.forward-headers-strategy=framework (see application.properties) so remoteAddr reflects
 * the real client, not the proxy.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    static final String NOTIFY_PATH = "/api/results/notify";
    static final String REMINDERS_PATH = "/api/reminders";

    private final boolean enabled;
    private final int publicRequestsPerMinute;
    private final int notifyRequestsPerHour;
    private final Clock clock;

    /** windowStart epoch-seconds + count, one entry per (scope, client IP). */
    private static final class Window {
        long windowStart;
        int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    // Keeps an unbounded attacker (spoofed source IPs) from growing the map forever; on
    // overflow the whole map resets, which briefly re-admits limited clients — an acceptable
    // trade against unbounded memory for a filter this simple.
    private static final int MAX_TRACKED_CLIENTS = 100_000;

    @org.springframework.beans.factory.annotation.Autowired
    public RateLimitFilter(
            @Value("${ratelimit.enabled:true}") boolean enabled,
            @Value("${ratelimit.public.requests-per-minute:100}") int publicRequestsPerMinute,
            @Value("${ratelimit.notify.requests-per-hour:5}") int notifyRequestsPerHour) {
        this(enabled, publicRequestsPerMinute, notifyRequestsPerHour, Clock.systemUTC());
    }

    RateLimitFilter(boolean enabled, int publicRequestsPerMinute, int notifyRequestsPerHour, Clock clock) {
        this.enabled = enabled;
        this.publicRequestsPerMinute = publicRequestsPerMinute;
        this.notifyRequestsPerHour = notifyRequestsPerHour;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Both email-sending endpoints share the strict hourly budget.
        String uri = request.getRequestURI();
        boolean isNotify = NOTIFY_PATH.equals(uri) || REMINDERS_PATH.equals(uri);
        int limit = isNotify ? notifyRequestsPerHour : publicRequestsPerMinute;
        long windowSeconds = isNotify ? 3600 : 60;
        String key = (isNotify ? "notify:" : "public:") + request.getRemoteAddr();

        if (!tryAcquire(key, limit, windowSeconds)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean tryAcquire(String key, int limit, long windowSeconds) {
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.clear();
        }
        long now = clock.instant().getEpochSecond();
        Window window = windows.computeIfAbsent(key, k -> new Window());
        synchronized (window) {
            if (now - window.windowStart >= windowSeconds) {
                window.windowStart = now;
                window.count = 0;
            }
            if (window.count >= limit) {
                return false;
            }
            window.count++;
            return true;
        }
    }
}
