package com.alfre.DHHotel.usecase;

public final class RegexConstants {
    public static final String PHONE_REGEX = "^(?=(?:\\D*\\d){9,15}\\D*$)\\+?[\\d\\s]+$";

    private RegexConstants() {
        // Evita la instanciación
    }
}