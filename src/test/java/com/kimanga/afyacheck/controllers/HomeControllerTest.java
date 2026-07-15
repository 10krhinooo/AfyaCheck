package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeControllerTest {

    private final SessionService sessionService = mock(SessionService.class);
    private final HomeController controller = new HomeController(sessionService);

    @Test
    void homePageAddsSessionIdToModel() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("http-sid");
        when(sessionService.createOrGetSession("http-sid")).thenReturn("app-sid");
        Model model = new ExtendedModelMap();

        String view = controller.homePage(session, model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("sessionId")).isEqualTo("app-sid");
    }

    @Test
    void homePageStillReturnsHomeOnException() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenThrow(new RuntimeException("boom"));
        Model model = new ExtendedModelMap();

        String view = controller.homePage(session, model);

        assertThat(view).isEqualTo("home");
    }

    @Test
    void homePageRedirectDelegatesToHomePage() {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn("http-sid-2");
        when(sessionService.createOrGetSession("http-sid-2")).thenReturn("app-sid-2");
        Model model = new ExtendedModelMap();

        String view = controller.homePageRedirect(session, model);

        assertThat(view).isEqualTo("home");
    }
}
