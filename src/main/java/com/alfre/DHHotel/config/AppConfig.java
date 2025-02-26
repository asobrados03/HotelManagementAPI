package com.alfre.DHHotel.config;

import com.alfre.DHHotel.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration class for application-wide authentication and security settings.
 * This class defines beans for authentication management, user details service,
 * and password encoding.
 *
 * <p>Uses Lombok's {@code @RequiredArgsConstructor} to inject dependencies.</p>
 *
 * @author Alfredo Sobrados González
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {
    private final UserRepository userRepository;

    /**
     * Provides the {@link AuthenticationManager} bean.
     *
     * @param config the authentication configuration
     * @return an instance of {@code AuthenticationManager}
     * @throws Exception if an error occurs while retrieving the authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides the {@link AuthenticationProvider} bean.
     * Configures the authentication provider with a custom user details service and password encoder.
     *
     * @return an instance of {@code AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailService());
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    /**
     * Provides the {@link PasswordEncoder} bean.
     * Uses BCrypt hashing to securely store and validate passwords.
     *
     * @return an instance of {@code PasswordEncoder} using {@code BCryptPasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides the {@link UserDetailsService} bean.
     * Retrieves user details from the database by email.
     *
     * @return an instance of {@code UserDetailsService}
     */
    @Bean
    public UserDetailsService userDetailService() {
        return email -> userRepository.getUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}