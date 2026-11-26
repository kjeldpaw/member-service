package dk.wandywharang.service;

import dk.wandywharang.entity.MemberEntity;
import dk.wandywharang.repository.MemberRepository;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

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

    private boolean isAdmin() {
        return securityIdentity.hasRole("admin");
    }

    private boolean isInstructor() {
        return securityIdentity.hasRole("instructor");
    }

    /**
     * Lists the Members the current user can see.
     */
    public Uni<List<MemberEntity>> findAllVisible() {
        if (isAdmin()) {
            return repository.findAll().list();
        }
        if (!isInstructor()) {
            return profile().map(List::of);
        }
        return profile().chain(repository::findSharingClubWith);
    }

    public Uni<MemberEntity> findVisible(UUID id) {
        if (isAdmin()) {
            return find(id);
        }
        if (!isInstructor()) {
            return profile().map(Unchecked.function(profile -> {
                if (!profile.getId().equals(id)) {
                    throw notFound(id);
                }
                return profile;
            }));
        }
        return find(id).chain(member -> requireSharedClub(member, () -> notFound(id)));
    }

    public Uni<MemberEntity> findEditable(UUID id) {
        if (isAdmin()) {
            return find(id);
        }
        if (!isInstructor()) {
            return Uni.createFrom().failure(new ForbiddenException("Not allowed to edit Members"));
        }
        return find(id).chain(member -> requireSharedClub(member,
                () -> new ForbiddenException(String.format("Member with id = %s shares no Club with you", id))));
    }

    /**
     * Only Admins may delete a Member entirely.
     */
    public Uni<MemberEntity> findDeletable(UUID id) {
        if (!isAdmin()) {
            return Uni.createFrom().failure(new ForbiddenException("Only Admins may delete Members"));
        }
        return find(id);
    }

    /**
     * Fails unless the current user may add Members to all the given Clubs: Admins to any Club, Instructors to their
     * own.
     */
    public Uni<Void> requireEditableClubs(Set<UUID> clubIds) {
        if (isAdmin()) {
            return Uni.createFrom().voidItem();
        }
        if (!isInstructor()) {
            return Uni.createFrom().failure(new ForbiddenException("Not allowed to edit Members"));
        }
        return profile().invoke(Unchecked.consumer(profile -> {
            final var notOwn = Ids.notIn(clubIds, profile.clubIds());
            if (!notOwn.isEmpty()) {
                throw new ForbiddenException(String.format("Clubs with ids = %s are not your Clubs", notOwn));
            }
        })).replaceWithVoid();
    }

    /**
     * Loads a Member whose membership of the given Club the current user may change: Admins any Member and any Club,
     * Instructors any Member but only their own Clubs. The Member need not share a Club with the Instructor, so an
     * Instructor can pull an existing Member into their Club.
     */
    public Uni<MemberEntity> findForClubChange(UUID id, UUID clubId) {
        return requireEditableClubs(Set.of(clubId)).chain(() -> find(id));
    }

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

    private static NotFoundException notFound(UUID id) {
        return new NotFoundException(String.format("Member with id = %s not found", id));
    }
}
