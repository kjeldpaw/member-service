package dk.wandywharang.service;

import java.util.UUID;

/**
 * Removing the Club would leave the Member without any, but every Member belongs to at least one Club. Deleting the
 * Member is the way to remove them entirely.
 */
public class LastClubException extends RuntimeException {

    public LastClubException(UUID memberId) {
        super(String.format("Member with id = %s must belong to at least one Club", memberId));
    }
}
