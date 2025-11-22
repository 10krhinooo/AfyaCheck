package com.kimanga.afyacheck.controllers;

import com.kimanga.afyacheck.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final SessionService sessionService;

    public HomeController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/")
    public String homePage(HttpSession httpSession, Model model) {
        try {
            // Create or get session when user visits homepage
            String sessionId = httpSession.getId();
            String createdSessionId = sessionService.createOrGetSession(sessionId);

            logger.info("Created session on homepage: {} for HTTP session: {}", createdSessionId, sessionId);

            model.addAttribute("sessionId", createdSessionId);
            return "home";

        } catch (Exception e) {
            logger.error("Error creating session on homepage", e);
            // Still return home page even if session creation fails
            return "home";
        }
    }

    @GetMapping("/home")
    public String homePageRedirect(HttpSession httpSession, Model model) {
        return homePage(httpSession, model);
    }
}