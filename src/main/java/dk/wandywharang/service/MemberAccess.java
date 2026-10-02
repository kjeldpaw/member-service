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
        if (!securityIdentity.hasRole("instructor")) {
            return Uni.createFrom().failure(new ForbiddenException("Not allowed to edit members"));
        }
        return find(id).chain(member -> requireSameClub(member,
                () -> new ForbiddenException(String.format("Member with id = %s is not in your club", id))));
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
