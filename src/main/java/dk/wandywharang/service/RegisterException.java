package dk.wandywharang.service;

import jakarta.ws.rs.WebApplicationException;

public class RegisterException extends WebApplicationException {

    public RegisterException(int status, String message) {
        super(message, status);
    }
}
