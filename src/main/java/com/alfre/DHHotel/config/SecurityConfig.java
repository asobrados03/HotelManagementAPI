package com.alfre.DHHotel.config;

import com.alfre.DHHotel.adapter.security.jwt.JWTAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * Security configuration class for the application.
 * It defines security policies, authentication providers, and JWT-based authentication.
 *
 * <p>Key security features include:</p>
 * <ul>
 *   <li>Disabling CSRF protection (for stateless APIs).</li>
 *   <li>Configuring security headers (CSP, XSS Protection).</li>
 *   <li>Defining role-based access control (RBAC) for API endpoints.</li>
 *   <li>Using JWT authentication with stateless session management.</li>
 * </ul>
 *
 * <p>This class uses the following annotations:</p>
 * <ul>
 *   <li>{@code @Configuration} - Marks this as a Spring configuration class.</li>
 *   <li>{@code @EnableWebSecurity} - Enables Spring Security.</li>
 *   <li>{@code @RequiredArgsConstructor} - Generates a constructor for final fields (Lombok).</li>
 * </ul>
 *
 * @author Alfredo Sobrados González
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authProvider;

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Disables CSRF (stateless APIs don't require CSRF protection).</li>
     *   <li>Configures security headers, including Content Security Policy (CSP) and XSS protection.</li>
     *   <li>Defines role-based authorization for different API endpoints.</li>
     *   <li>Sets session management to {@code STATELESS} for JWT-based authentication.</li>
     *   <li>Registers the custom JWT authentication filter before the standard {@link UsernamePasswordAuthenticationFilter}.</li>
     * </ul>
     *
     * @param http the {@code HttpSecurity} object to configure security settings
     * @return a {@code SecurityFilterChain} instance
     * @throws Exception if an error occurs during security configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)

                // Security headers configuration
                .headers(headers -> headers
                        // Content Security Policy (CSP) settings
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'; script-src 'self'; object-src 'none';")
                        )
                        // Adds the X-XSS-Protection header manually (for compatibility with older browsers)
                        .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register/client",
                                "/api/auth/login",
                                "/api/public/**",
                                "/error",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/openapi.yml"
                        ).permitAll()
                        .requestMatchers("/api/client/**").hasRole("CLIENT")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/superadmin/**", "/api/auth/register/admin").hasRole("SUPERADMIN")
                        .anyRequest().authenticated()
                )

                // Session management (stateless for JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authentication provider setup
                .authenticationProvider(authProvider)

                // Register JWT authentication filter before username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}