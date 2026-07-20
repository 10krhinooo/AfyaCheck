package com.kimanga.afyacheck.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // frontend/dist bundles one JS/CSS build shared by both "/" (prerendered by
        // vite-react-ssg at build time) and everything under /app/** (a pure client-rendered
        // SPA, no SEO value — see vite.config.ts ssgOptions.includedRoutes). Deep links/refreshes
        // under /app/** don't correspond to a static file, so forward them to app-shell.html —
        // index.html stripped of "/"'s prerendered markup and its data-server-rendered flag (see
        // scripts/generate-app-shell.mjs) — rather than to index.html itself: forwarding to
        // index.html would make the client bundle try to *hydrate* the Landing page's markup
        // against whatever /app/** route actually matched, which is guaranteed to mismatch and
        // throws a React error #418 on every load. app-shell.html has an empty root, so the
        // bundle does a plain client-side render() there instead.
        registry.addViewController("/app/{path:[^\\.]*}").setViewName("forward:/app-shell.html");
        registry.addViewController("/app/**").setViewName("forward:/app-shell.html");

        // About/FAQ/Privacy/Terms are each individually prerendered to their own static HTML
        // file at build time (see vite.config.ts ssgOptions.includedRoutes), unlike /app/**'s
        // single shared shell — forward the clean path to that specific file rather than to
        // index.html.
        registry.addViewController("/about").setViewName("forward:/about.html");
        registry.addViewController("/faq").setViewName("forward:/faq.html");
        registry.addViewController("/privacy").setViewName("forward:/privacy.html");
        registry.addViewController("/terms").setViewName("forward:/terms.html");

        // Education pages: /learn prerenders to learn.html and each topic to
        // learn/<slug>.html (flat dirStyle). View controllers can't substitute path
        // variables into the view name, so each prerendered topic gets an explicit mapping —
        // keep in sync with vite.config.ts ssgOptions.includedRoutes.
        registry.addViewController("/learn").setViewName("forward:/learn.html");
        registry.addViewController("/learn/sti-basics").setViewName("forward:/learn/sti-basics.html");
        registry.addViewController("/learn/hiv-prevention").setViewName("forward:/learn/hiv-prevention.html");
        registry.addViewController("/learn/testing").setViewName("forward:/learn/testing.html");
    }
}
