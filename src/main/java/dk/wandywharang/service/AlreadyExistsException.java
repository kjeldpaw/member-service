package dk.wandywharang.service;

/**
 * The person being registered already exists, so the client can offer to add them to a Club instead.
 */
public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String email) {
        super(String.format("A person with email %s already exists", email));
    }
}
