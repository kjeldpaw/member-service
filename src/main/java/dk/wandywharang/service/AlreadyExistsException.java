package dk.wandywharang.service;

import java.util.UUID;

/**
 * The person being registered already exists, so the client can offer to add them to a Club instead.
 */
public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String email) {
        super(String.format("A person with email %s already exists", email));
    }

    public AlreadyExistsException(UUID id) {
        super(String.format("A Member with id = %s already exists", id));
    }
}
