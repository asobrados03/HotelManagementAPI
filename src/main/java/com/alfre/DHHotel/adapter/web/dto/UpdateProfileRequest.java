package com.alfre.DHHotel.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) for updating a user's profile.
 * Encapsulates the necessary fields for modifying user profile information.
 *
 * <p>This class utilizes Lombok annotations to reduce boilerplate code:</p>
 * <ul>
 *   <li>{@code @Data} - Generates getters, setters, toString, equals, and hashCode methods.</li>
 *   <li>{@code @AllArgsConstructor} - Generates a constructor with all fields.</li>
 *   <li>{@code @NoArgsConstructor} - Generates a no-argument constructor.</li>
 * </ul>
 *
 * @author Alfredo
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    /**
     * The updated first name of the user.
     */
    private String firstName;

    /**
     * The updated last name of the user.
     */
    private String lastName;

    /**
     * The updated phone number of the user.
     */
    private String phone;
}
