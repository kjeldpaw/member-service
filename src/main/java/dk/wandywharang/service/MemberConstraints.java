package dk.wandywharang.service;

import dk.wandywharang.entity.MemberEntity;
import org.hibernate.exception.ConstraintViolationException;

/**
 * The database's unique constraints on Members, which hold even when two requests pass the service's own checks at
 * the same moment.
 */
final class MemberConstraints {
    /**
     * The primary key of members: one Member per Keycloak user.
     */
    static final String PRIMARY_KEY = "members_pkey";

    /**
     * The unique index on lower(email): one Member per email, ignoring case, as Keycloak does.
     */
    static final String EMAIL = "members_email_lower_key";

    private MemberConstraints() {
    }

    /**
     * The {@link AlreadyExistsException} for a failure that broke one of the constraints when storing the Member, or
     * the failure itself otherwise.
     */
    static Throwable alreadyExists(Throwable failure, MemberEntity member) {
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                switch (violation.getConstraintName()) {
                    case PRIMARY_KEY:
                        return new AlreadyExistsException(member.getId());
                    case EMAIL:
                        return new AlreadyExistsException(member.getEmail());
                    default:
                        return failure;
                }
            }
        }
        return failure;
    }
}
