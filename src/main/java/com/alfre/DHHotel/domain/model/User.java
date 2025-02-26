package com.alfre.DHHotel.domain.model;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Entity class representing a user in the system.
 * Implements {@code UserDetails} to integrate with Spring Security.
 *
 * <p>It uses Lombok annotations to reduce boilerplate code:</p>
 * <ul>
 *   <li>{@code @Builder} - Enables the builder pattern for object creation.</li>
 *   <li>{@code @Setter} - Generates setter methods for all fields.</li>
 *   <li>{@code @AllArgsConstructor} - Generates a constructor with all fields.</li>
 *   <li>{@code @NoArgsConstructor} - Generates a no-argument constructor.</li>
 *   <li>{@code @ToString(exclude = "password")} - Excludes the password from the {@code toString()} method.</li>
 * </ul>
 *
 * <p>This class stores user authentication details and integrates with Spring Security.</p>
 *
 * @author Alfredo Sobrados González
 */
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "password")
public class User implements UserDetails {
    /**
     * The prefix required by Spring Security for role-based access control.
     */
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * The unique identifier for the user.
     */
    public long id;

    /**
     * The email address of the user, used as a unique identifier.
     */
    public String email;

    /**
     * The hashed password of the user.
     */
    public String password;

    /**
     * The role assigned to the user (CLIENT, ADMIN, SUPERADMIN).
     */
    public Role role; // Enum common for clients and administrators

    /**
     * Retrieves the authorities (roles) granted to the user.
     * This method ensures that the role has the required "ROLE_" prefix for Spring Security.
     *
     * @return a collection of granted authorities, or an empty list if no role is assigned.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }

    /**
     * Retrieves the user's password.
     *
     * @return the password.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Retrieves the username, which is the user's email.
     *
     * @return the email of the user.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the user's account is expired.
     * Always returns {@code true} as expiration is not managed in this implementation.
     *
     * @return {@code true}, meaning the account is always valid.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user's account is locked.
     * Always returns {@code true} as account locking is not managed in this implementation.
     *
     * @return {@code true}, meaning the account is never locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) are expired.
     * Always returns {@code true} as credential expiration is not managed in this implementation.
     *
     * @return {@code true}, meaning credentials are always valid.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled.
     * Always returns {@code true} as user enabling/disabling is not managed in this implementation.
     *
     * @return {@code true}, meaning the user is always enabled.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}