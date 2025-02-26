package com.alfre.DHHotel.adapter.web.dto;

import lombok.Setter;

/**
 * Data Transfer Object (DTO) for updating user credentials.
 * This class is used to encapsulate the new password and email for a user update request.
 *
 * <p>It uses the {@code @Setter} annotation from Lombok to generate setter methods automatically.</p>
 *
 * @author Alfredo
 */
@Setter
public class UserDTO {
    /**
     * The new password for the user.
     */
    public String newPassword;

    /**
     * The new email address for the user.
     */
    public String newEmail;
}