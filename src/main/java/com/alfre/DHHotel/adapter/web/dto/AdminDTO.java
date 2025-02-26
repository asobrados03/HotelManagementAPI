package com.alfre.DHHotel.adapter.web.dto;

import com.alfre.DHHotel.domain.model.Role;
import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for an administrator.
 * Contains essential administrator information such as email, role, and name.
 * <p>
 * This class uses Lombok annotations to generate boilerplate code:
 * <ul>
 *  <li>@Builder: Generates a builder for creating instances.</li>
 *  <li>@Data: Generates getters, setters, toString, equals, and hashCode methods.</li>
 * </ul>
 *
 * @author Alfredo Sobrados González
 */
@Builder
@Data
public class AdminDTO {
    /**
     * The email address of the administrator.
     */
    public String email;

    /**
     * The role assigned to the administrator.
     */
    public Role role;

    /**
     * The full name of the administrator.
     */
    public String name;
}
