package com.kimanga.afyacheck.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // frontend/dist is bundled into static resources at build time as a single SPA shell
        // (one index.html, one JS bundle — the same bundle prerenders "/" at build time via
        // vite-react-ssg AND drives client-side React Router for everything under /app/**).
        // Deep links/refreshes under /app/** don't correspond to a static file, so forward
        // them to that same index.html and let React Router take over once it hydrates.
        registry.addViewController("/app/{path:[^\\.]*}").setViewName("forward:/index.html");
        registry.addViewController("/app/**").setViewName("forward:/index.html");
    }
}
