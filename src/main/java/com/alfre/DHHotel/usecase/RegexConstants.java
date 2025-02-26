package com.alfre.DHHotel.usecase;

/**
 * Utility class that holds regular expressions used for validation.
 * <p>
 * This class is final and has a private constructor to prevent instantiation.
 * </p>
 *
 * @author Alfredo Sobrados González
 */
public final class RegexConstants {

    /**
     * Regular expression for validating phone numbers.
     * <p>
     * This regex ensures:
     * <ul>
     *     <li>At least 9 and at most 15 digits.</li>
     *     <li>Optional leading '+' for international numbers.</li>
     *     <li>Allows spaces and other non-digit characters but ensures the required digit count.</li>
     * </ul>
     * </p>
     */
    public static final String PHONE_REGEX = "^(?=(?:\\D*\\d){9,15}\\D*$)\\+?[\\d\\s]+$";

    /**
     * Private constructor to prevent instantiation.
     */
    private RegexConstants() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }
}