package com.alfre.DHHotel.domain.model;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "password")
public class User implements UserDetails {
    private static final String ROLE_PREFIX = "ROLE_";

    public long id;
    public String email;
    public String password;
    public Role role; // Enum común para clientes y administradores

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        // Agregar el prefijo ROLE_ que requiere Spring Security
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}