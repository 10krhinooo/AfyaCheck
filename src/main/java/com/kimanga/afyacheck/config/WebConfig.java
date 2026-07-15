package com.kimanga.afyacheck.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        // Legacy Thymeleaf MVC routes only; revisit/remove once those routes are decommissioned
        // by the React migration (see /app/** below, which must stay cacheable for hashed assets).
        interceptor.addCacheMapping(null, "/admin/**", "/dashboard/**");
        interceptor.setCacheSeconds(0);
        registry.addInterceptor(interceptor);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // The React SPA (frontend/dist, bundled into static resources at build time) owns
        // every path under /app/**. Forward all of them to its index.html so React Router
        // can resolve deep links on a full page load/refresh instead of 404ing.
        registry.addViewController("/app/{path:[^\\.]*}").setViewName("forward:/app/index.html");
        registry.addViewController("/app/**/{path:[^\\.]*}").setViewName("forward:/app/index.html");
    }
}