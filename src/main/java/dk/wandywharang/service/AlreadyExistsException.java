package dk.wandywharang.service;

import dk.wandywharang.api.ErrorMessage;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * The person being registered already exists, so the client can offer to add them to a Club instead.
 */
public class AlreadyExistsException extends WebApplicationException {

    public AlreadyExistsException(String email) {
        super(Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorMessage(String.format("A person with email %s already exists", email)))
                .build());
    }
}
