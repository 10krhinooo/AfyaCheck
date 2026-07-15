package com.kimanga.afyacheck.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        interceptor.addCacheMapping(null, "/admin/**", "/dashboard/**");
        interceptor.setCacheSeconds(0);
        registry.addInterceptor(interceptor);
    }

    /**
     * The SPA root (exactly /app or /app/) is handled separately from the
     * resource resolver below: an empty resourcePath at that resolver hits
     * a Spring resource-handling edge case (NoResourceFoundException,
     * "No static resource .") rather than reaching custom fallback logic,
     * so forwarding it here is more reliable than trying to special-case
     * it inside the resolver.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/app").setViewName("forward:/app/index.html");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
    }

    /**
     * Serves the React SPA's static files under /app/**, falling back to
     * index.html for any path that isn't an actual built asset (e.g.
     * /app/dashboard) so React Router can own client-side routes and
     * survive a refresh or deep link.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/app/**")
                .addResourceLocations("classpath:/static/app/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws java.io.IOException {
                        if (resourcePath.isEmpty()) {
                            return null;
                        }
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }
}