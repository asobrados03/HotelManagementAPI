package com.alfre.DHHotel.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for login requests.
 * This class encapsulates the necessary credentials for user authentication.
 *
 * <p>It utilizes Lombok annotations for reducing boilerplate code:</p>
 * <ul>
 *   <li>{@code @Data} - Generates getters, setters, toString, equals, and hashCode methods.</li>
 *   <li>{@code @Builder} - Provides a builder pattern for creating instances.</li>
 *   <li>{@code @AllArgsConstructor} - Generates a constructor with all fields.</li>
 *   <li>{@code @NoArgsConstructor} - Generates a no-argument constructor.</li>
 * </ul>
 *
 * @author Alfredo Sobrados González
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    /**
     * The username of the user (typically their email).
     */
    private String username; // email

    /**
     * The password associated with the user account.
     */
    private String password;
}
