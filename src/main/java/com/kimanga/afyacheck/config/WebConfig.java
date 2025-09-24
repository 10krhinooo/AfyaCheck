package com.kimanga.afyacheck.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//import javax.servlet.*;
import java.io.IOException;

@Configuration
public class WebConfig {

    @Bean
    public Filter cacheControlFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
                httpResponse.setHeader("Pragma", "no-cache"); // HTTP 1.0
                httpResponse.setDateHeader("Expires", 0); // Proxies
                chain.doFilter(request, response);
            }
        };
    }
}
