package com.alfre.DHHotel.domain.model;

/**
 * Enum representing the different roles a user can have in the system.
 *
 * <p>Possible values:</p>
 * <ul>
 *   <li>{@code CLIENT} - A regular user who can make reservations.</li>
 *   <li>{@code ADMIN} - An administrator who manages reservations and users.</li>
 *   <li>{@code SUPERADMIN} - A super administrator with full system access.</li>
 * </ul>
 *
 * This enum is used for access control and authorization management.
 *
 * @author Alfredo Sobrados González
 */
public enum Role {
    /**
     * A regular user who can make reservations.
     */
    CLIENT,

    /**
     * An administrator who manages reservations and users.
     */
    ADMIN,

    /**
     * A super administrator with full system access.
     */
    SUPERADMIN
}
