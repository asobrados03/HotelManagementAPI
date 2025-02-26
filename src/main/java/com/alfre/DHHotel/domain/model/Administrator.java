package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity class representing an administrator.
 * This class maps to the administrator table in the database and contains essential attributes.
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
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class Administrator {
    /**
     * The unique identifier for the administrator.
     */
    public long id;

    /**
     * The user ID that links this administrator to a record in the users table.
     */
    public long user_id; // Foreign key to the users table

    /**
     * The full name of the administrator.
     */
    public String name;
}