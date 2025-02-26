package com.alfre.DHHotel.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for authentication responses.
 * This class encapsulates the JWT token returned after a successful authentication.
 *
 * <p>It uses Lombok annotations for reducing boilerplate code:</p>
 * <ul>
 *   <li>{@code @Data} - Generates getters, setters, toString, equals, and hashCode methods.</li>
 *   <li>{@code @Builder} - Provides a builder pattern for creating instances.</li>
 *   <li>{@code @NoArgsConstructor} - Generates a no-argument constructor.</li>
 *   <li>{@code @AllArgsConstructor} - Generates a constructor with all fields.</li>
 * </ul>
 *
 * @author Alfredo Sobrados González
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    /**
     * The JWT token issued upon successful authentication.
     */
    private String token;
}