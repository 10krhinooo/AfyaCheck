package com.kimanga.afyacheck.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private static final Instant NOW = Instant.parse("2026-07-20T12:00:00Z");

    private MockHttpServletResponse request(RateLimitFilter filter, String uri, String ip)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void allowsRequestsUnderTheLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 3, 5, Clock.fixed(NOW, ZoneOffset.UTC));
        for (int i = 0; i < 3; i++) {
            assertThat(request(filter, "/api/questions/next", "1.2.3.4").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void rejectsRequestsOverTheLimitWith429() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 2, 5, Clock.fixed(NOW, ZoneOffset.UTC));
        request(filter, "/api/questions/next", "1.2.3.4");
        request(filter, "/api/questions/next", "1.2.3.4");
        MockHttpServletResponse rejected = request(filter, "/api/questions/next", "1.2.3.4");
        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        assertThat(rejected.getContentAsString()).contains("Too many requests");
    }

    @Test
    void limitsAreTrackedPerClientIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 5, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(request(filter, "/api/questions/next", "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(request(filter, "/api/questions/next", "2.2.2.2").getStatus()).isEqualTo(200);
        assertThat(request(filter, "/api/questions/next", "1.1.1.1").getStatus()).isEqualTo(429);
    }

    @Test
    void notifyEndpointHasItsOwnStricterHourlyLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 100, 2, Clock.fixed(NOW, ZoneOffset.UTC));
        request(filter, RateLimitFilter.NOTIFY_PATH, "1.2.3.4");
        request(filter, RateLimitFilter.NOTIFY_PATH, "1.2.3.4");
        MockHttpServletResponse rejected = request(filter, RateLimitFilter.NOTIFY_PATH, "1.2.3.4");
        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("3600");
        // The generous public limit is unaffected by the notify counter.
        assertThat(request(filter, "/api/questions/next", "1.2.3.4").getStatus()).isEqualTo(200);
    }

    @Test
    void windowResetsAfterItElapses() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        RateLimitFilter filter = new RateLimitFilter(true, 1, 5, clock);
        assertThat(request(filter, "/api/questions/next", "1.2.3.4").getStatus()).isEqualTo(200);
        assertThat(request(filter, "/api/questions/next", "1.2.3.4").getStatus()).isEqualTo(429);
        clock.advance(Duration.ofSeconds(61));
        assertThat(request(filter, "/api/questions/next", "1.2.3.4").getStatus()).isEqualTo(200);
    }

    @Test
    void skipsNonApiPathsAndDisabledFilter() {
        RateLimitFilter enabled = new RateLimitFilter(true, 1, 1, Clock.fixed(NOW, ZoneOffset.UTC));
        MockHttpServletRequest staticAsset = new MockHttpServletRequest("GET", "/app/dashboard");
        staticAsset.setRequestURI("/app/dashboard");
        assertThat(enabled.shouldNotFilter(staticAsset)).isTrue();

        MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/questions/next");
        api.setRequestURI("/api/questions/next");
        assertThat(enabled.shouldNotFilter(api)).isFalse();

        RateLimitFilter disabled = new RateLimitFilter(false, 1, 1, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThat(disabled.shouldNotFilter(api)).isTrue();
    }

    /** Minimal adjustable Clock for window-expiry tests. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
