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
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Loads Members on behalf of the current user. Admins can see and edit all Members. Everyone else can see the Members
 * who share at least one Club with them, and Instructors can also edit those. A user's Clubs are the Clubs of their
 * own Member record.
 */
@RequestScoped
@RequiredArgsConstructor
public class MemberAccess {
    private final SecurityIdentity securityIdentity;
    private final MemberRepository repository;

    public boolean isAdmin() {
        return securityIdentity.hasRole("admin");
    }

    private boolean isInstructor() {
        return securityIdentity.hasRole("instructor");
    }

    public Uni<MemberEntity> findVisible(UUID id) {
        if (isAdmin()) {
            return find(id);
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
            final var notOwn = new TreeSet<>(clubIds);
            notOwn.removeAll(profile.clubIds());
            if (!notOwn.isEmpty()) {
                throw new ForbiddenException(String.format("Clubs with ids = %s are not your Clubs", notOwn));
            }
        })).replaceWithVoid();
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
