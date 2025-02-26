package com.alfre.DHHotel.adapter.web.dto;

import com.alfre.DHHotel.domain.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for user registration requests.
 * This class encapsulates the necessary details for creating a new user account.
 *
 * <p>It utilizes Lombok annotations for reducing boilerplate code:</p>
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
public class RegisterRequest {
    /**
     * The username of the user (typically their email).
     */
    private String username; // email

    /**
     * The password associated with the user account.
     */
    private String password;

    /**
     * The full name of the user.
     */
    private String name;

    /**
     * The first name of the user.
     */
    private String first_name;

    /**
     * The last name of the user.
     */
    private String last_name;

    /**
     * The phone number of the user.
     */
    private String phone;

    /**
     * The role assigned to the user (e.g., ADMIN, CLIENT, SUPERADMIN).
     */
    private Role role;
}

