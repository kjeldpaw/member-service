package dk.wandywharang.service;

import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Loads Members on behalf of the current user. Admins can see and edit all Members. Instructors can see and edit the
 * Members who share at least one Club with them. A plain Member (neither Admin nor Instructor) can see only their own
 * Member record, and edit none. A user's Clubs are the Clubs of their own Member record. A Member the user may not see
 * is reported as not found, so its existence is not revealed.
 */
@RequestScoped
@RequiredArgsConstructor
public class MemberAccess {
    private final SecurityIdentity securityIdentity;
    private final MemberRepository repository;

    /**
     * How much of the register the current user may see and edit. A user with several roles gets the widest.
     */
    private enum Level { ADMIN, INSTRUCTOR, PLAIN_MEMBER }

    private Level level() {
        if (securityIdentity.hasRole("admin")) {
            return Level.ADMIN;
        }
        if (securityIdentity.hasRole("instructor")) {
            return Level.INSTRUCTOR;
        }
        return Level.PLAIN_MEMBER;
    }

    /**
     * Lists the Members the current user can see.
     */
    public Uni<List<MemberEntity>> findAllVisible() {
        return switch (level()) {
            case ADMIN -> repository.findAll().list();
            case INSTRUCTOR -> profile().chain(repository::findSharingClubWith);
            case PLAIN_MEMBER -> profile().map(List::of);
        };
    }

    public Uni<MemberEntity> findVisible(UUID id) {
        return switch (level()) {
            case ADMIN -> find(id);
            case INSTRUCTOR -> find(id).chain(member -> requireSharedClub(member, () -> notFound(id)));
            case PLAIN_MEMBER -> profile().map(Unchecked.function(profile -> {
                if (!profile.getId().equals(id)) {
                    throw notFound(id);
                }
                return profile;
            }));
        };
    }

    public Uni<MemberEntity> findEditable(UUID id) {
        return switch (level()) {
            case ADMIN -> find(id);
            case INSTRUCTOR -> find(id).chain(member -> requireSharedClub(member,
                    () -> new ForbiddenException(String.format("Member with id = %s shares no Club with you", id))));
            case PLAIN_MEMBER -> notAllowedToEdit();
        };
    }

    /**
     * Only Admins may delete a Member entirely.
     */
    public Uni<MemberEntity> findDeletable(UUID id) {
        if (level() != Level.ADMIN) {
            return Uni.createFrom().failure(new ForbiddenException("Only Admins may delete Members"));
        }
        return find(id);
    }

    /**
     * Fails unless the current user may add Members to all the given Clubs: Admins to any Club, Instructors to their
     * own.
     */
    public Uni<Void> requireEditableClubs(Set<UUID> clubIds) {
        return switch (level()) {
            case ADMIN -> Uni.createFrom().voidItem();
            case INSTRUCTOR -> profile().invoke(Unchecked.consumer(profile -> {
                final var notOwn = Ids.notIn(clubIds, profile.clubIds());
                if (!notOwn.isEmpty()) {
                    throw new ForbiddenException(String.format("Clubs with ids = %s are not your Clubs", notOwn));
                }
            })).replaceWithVoid();
            case PLAIN_MEMBER -> notAllowedToEdit();
        };
    }

    /**
     * Loads a Member whose membership of the given Club the current user may change: Admins any Member and any Club,
     * Instructors any Member but only their own Clubs. The Member need not share a Club with the Instructor, so an
     * Instructor can pull an existing Member into their Club.
     */
    public Uni<MemberEntity> findForClubChange(UUID id, UUID clubId) {
        return requireEditableClubs(Set.of(clubId)).chain(() -> find(id));
    }

    /**
     * The logged-in person's id and email, from their token. Fails with a bad request if the token has no email.
     */
    public Uni<Caller> caller() {
        final var principal = securityIdentity.getPrincipal();
        final String email = principal instanceof JsonWebToken token ? token.getClaim(Claims.email) : null;
        if (email == null || email.isBlank()) {
            return Uni.createFrom().failure(new BadRequestException("The token has no email"));
        }
        return Uni.createFrom().item(new Caller(UUID.fromString(principal.getName()), email));
    }

    /**
     * The logged-in person's own Member record, or not found if they have none.
     */
    public Uni<MemberEntity> profile() {
        final var name = securityIdentity.getPrincipal().getName();
        return repository.findById(UUID.fromString(name))
                .onItem().ifNull().failWith(() -> new NotFoundException(String.format("Profile with id = %s not found", name)));
    }

    private Uni<MemberEntity> find(UUID id) {
        return repository.findById(id)
                .onItem().ifNull().failWith(() -> notFound(id));
    }

    private Uni<MemberEntity> requireSharedClub(MemberEntity member, Supplier<RuntimeException> failure) {
        return profile().map(Unchecked.function(profile -> {
            if (Collections.disjoint(profile.clubIds(), member.clubIds())) {
                throw failure.get();
            }
            return member;
        }));
    }

    private static <T> Uni<T> notAllowedToEdit() {
        return Uni.createFrom().failure(new ForbiddenException("Not allowed to edit Members"));
    }

    private static NotFoundException notFound(UUID id) {
        return new NotFoundException(String.format("Member with id = %s not found", id));
    }
}
