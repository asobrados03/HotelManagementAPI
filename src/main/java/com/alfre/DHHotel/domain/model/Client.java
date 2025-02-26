package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity class representing a client.
 * This class maps to the client table in the database and contains essential attributes.
 *
 * <p>It uses Lombok annotations to reduce boilerplate code:</p>
 * <ul>
 *   <li>{@code @Builder} - Enables the builder pattern for object creation.</li>
 *   <li>{@code @AllArgsConstructor} - Generates a constructor with all fields.</li>
 *   <li>{@code @NoArgsConstructor} - Generates a no-argument constructor.</li>
 *   <li>{@code @Setter} - Generates setter methods for all fields.</li>
 * </ul>
 *
 * @author Alfredo Sobrados González
 */
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    /**
     * The unique identifier for the client.
     */
    public long id;

    /**
     * The user ID that links this client to a record in the users table.
     */
    public long user_id; // Foreign key to the users table

    /**
     * The first name of the client.
     */
    public String first_name;

    /**
     * The last name of the client.
     */
    public String last_name;

    /**
     * The phone number of the client.
     */
    public String phone;
}