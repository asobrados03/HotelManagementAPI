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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)

                // Configuración de cabeceras de seguridad
                .headers(headers -> headers
                        // Configuración de Content Security Policy (CSP)
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'; script-src 'self'; object-src 'none';")
                        )
                        // Agregamos el encabezado X-XSS-Protection manualmente (compatibilidad navegadores antiguos)
                        .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register/client", "/api/auth/login"
                                , "/api/public/**", "/error", "/swagger-ui.html",
                                "/swagger-ui/**", "/v3/api-docs/**", "/openapi.yml").permitAll()
                        .requestMatchers("/api/client/**").hasRole("CLIENT")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/superadmin/**", "/api/auth/register/admin").hasRole("SUPERADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}