package com.kimanga.afyacheck.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SecurityConfigTest {

    private final SecurityConfig.CsrfCookieFilter filter = new SecurityConfig.CsrfCookieFilter();

    @Test
    void rendersDeferredCsrfTokenSoItsCookieGetsWritten() throws ServletException, IOException {
        // CookieCsrfTokenRepository wraps the real token in a deferred supplier that only writes
        // the cookie once something calls getToken() -- verifying the mock's getToken() was
        // invoked confirms the filter forces that read, same as Spring's DeferredCsrfToken does.
        CsrfToken csrfToken = mock(CsrfToken.class);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setAttribute(CsrfToken.class.getName(), csrfToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(csrfToken).getToken();
        assertThat(chain.getRequest()).isEqualTo(request);
    }

    @Test
    void skipsWhenNoCsrfTokenOnRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isEqualTo(request);
    }
}
