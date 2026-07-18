package com.kimanga.afyacheck.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI afyaCheckOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AfyaCheck API")
                .description("STI/HIV risk assessment, admin, and health-center endpoints. "
                        + "Restricted to admins, same as the rest of /api/admin/**.")
                .version("v1"));
    }
}
