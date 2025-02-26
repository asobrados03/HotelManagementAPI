package com.alfre.DHHotel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * This class handles exceptions across the whole application.
 * It contains the attributes and methods for centralized exception handling.
 *
 * @author Alfredo Sobrados González
 */
@RestControllerAdvice
public class RutasHandler {

    private final Logger logger = LoggerFactory.getLogger(RutasHandler.class);

    /**
     * Handles NullPointerExceptions thrown by any controller method.
     * Logs the error message and returns a generic error message to the client.
     *
     * @param exception the NullPointerException that was thrown
     * @return a generic error message instructing the client to contact support
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleNullPointerException(NullPointerException exception) {
        logger.error(exception.getMessage());
        return "Internal error. Contact support";
    }
}