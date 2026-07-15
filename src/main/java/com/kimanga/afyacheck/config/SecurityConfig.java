package com.kimanga.afyacheck.config;

import com.kimanga.afyacheck.service.CustomOAuth2UserService;
import com.kimanga.afyacheck.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final Optional<ClientRegistrationRepository> clientRegistrationRepository;

    @Value("${security.remember-me.key}")
    private String rememberMeKey;

    @Value("${security.remember-me.token-validity-seconds}")
    private int rememberMeTokenValiditySeconds;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setDefaultFailureUrl("/login?error=true");
        return failureHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/register",
                                "/login",
                                "/forgot-password",
                                "/reset-password",
                                "/verify**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login").permitAll()
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true) // Redirect to dashboard
                        .failureHandler(authenticationFailureHandler())
                        .usernameParameter("username")
                        .passwordParameter("password")
                );

        // OAuth2 login is only wired up when at least one provider is configured
        // (see the commented-out registration properties in application.properties).
        // ClientRegistrationRepository doesn't exist as a bean otherwise, and
        // .oauth2Login() requires it unconditionally.
        if (clientRegistrationRepository.isPresent()) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/dashboard", true) // Redirect to dashboard for OAuth2 too
                    .failureUrl("/login?error=oauth")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService())
                    )
            );
        }

        http
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout") // Redirect to login with logout message
                        .invalidateHttpSession(true) // Invalidate session
                        .clearAuthentication(true) // Clear authentication
                        .deleteCookies("JSESSIONID", "afyacheck-remember-me") // Delete all cookies
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .tokenValiditySeconds(rememberMeTokenValiditySeconds)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName("afyacheck-remember-me")
                )
                .sessionManagement(session -> session
                        .sessionFixation().changeSessionId() // Change session ID on login
                        .maximumSessions(1) // Prevent multiple logins
                        .maxSessionsPreventsLogin(false) // Allow new login, invalidate old one
                )
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .frameOptions(frame -> frame.sameOrigin())
                )
                .userDetailsService(userDetailsService);

        return http.build();
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return new CustomOAuth2UserService();
    }
}