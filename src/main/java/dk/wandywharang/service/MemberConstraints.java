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
     * Translates a failure that broke one of the constraints when storing the Member into an
     * {@link AlreadyExistsException}, and returns any other failure unchanged.
     */
    static Throwable translateViolationOrPassThrough(Throwable failure, MemberEntity member) {
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return switch (violation.getConstraintName()) {
                    case PRIMARY_KEY -> new AlreadyExistsException(member.getId());
                    case EMAIL -> new AlreadyExistsException(member.getEmail());
                    default -> failure;
                };
            }
        }
        return failure;
    }
}
