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

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Loads members on behalf of the current user. Admins can see and edit all members, instructors can edit members of
 * their own club, and everyone else can only see members of their own club.
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
        return find(id).chain(member -> requireSameClub(member, () -> notFound(id)));
    }

    public Uni<MemberEntity> findEditable(UUID id) {
        if (isAdmin()) {
            return find(id);
        }
        if (!isInstructor()) {
            return Uni.createFrom().failure(new ForbiddenException("Not allowed to edit members"));
        }
        return find(id).chain(member -> requireSameClub(member,
                () -> new ForbiddenException(String.format("Member with id = %s is not in your club", id))));
    }

    /**
     * Fails unless the current user may add members to the given club: admins to any club, instructors to their own.
     */
    public Uni<Void> requireEditableClub(UUID clubId) {
        if (isAdmin()) {
            return Uni.createFrom().voidItem();
        }
        if (!isInstructor()) {
            return Uni.createFrom().failure(new ForbiddenException("Not allowed to edit members"));
        }
        return profile().invoke(Unchecked.consumer(profile -> {
            if (!profile.getClub().getId().equals(clubId)) {
                throw new ForbiddenException(String.format("Club with id = %s is not your club", clubId));
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

    private Uni<MemberEntity> requireSameClub(MemberEntity member, Supplier<RuntimeException> failure) {
        return profile().map(Unchecked.function(profile -> {
            if (!profile.getClub().getId().equals(member.getClub().getId())) {
                throw failure.get();
            }
            return member;
        }));
    }

    private static NotFoundException notFound(UUID id) {
        return new NotFoundException(String.format("Member with id = %s not found", id));
    }
}
