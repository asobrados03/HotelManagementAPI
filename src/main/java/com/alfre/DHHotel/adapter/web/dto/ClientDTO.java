package com.alfre.DHHotel.adapter.web.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for a client.
 * Encapsulates client-related information such as email, name, and contact details.
 *
 * <p>This class utilizes Lombok annotations to generate common boilerplate code:</p>
 * <ul>
 *   <li>{@code @Data} - Generates getters, setters, toString, equals, and hashCode methods.</li>
 *   <li>{@code @Builder} - Provides a builder pattern for creating instances.</li>
 * </ul>
 *
 * @author Alfredo
 */
@Builder
@Data
public class ClientDTO {
    /**
     * The email address of the client.
     */
    public String email;

    /**
     * The first name of the client.
     */
    public String firstName;

    /**
     * The last name of the client.
     */
    public String lastName;

    /**
     * The phone number of the client.
     */
    public String phone;
}

