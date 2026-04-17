package dk.wandywharang.service;

import jakarta.ws.rs.WebApplicationException;

public class AlreadyExistsException extends WebApplicationException {

    public AlreadyExistsException() {
        super("User already exists", 409);
    }
}
