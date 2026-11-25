package dk.wandywharang.repository;

import dk.wandywharang.entity.ClubEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class ClubRepository implements PanacheRepositoryBase<ClubEntity, UUID> {

    /**
     * Finds all the given Clubs, failing with not found if any of them don't exist.
     */
    public Uni<Set<ClubEntity>> findByIds(Set<UUID> ids) {
        return find("id in ?1", ids).list().map(clubs -> {
            final var missing = new TreeSet<>(ids);
            missing.removeAll(clubs.stream().map(ClubEntity::getId).collect(Collectors.toSet()));
            if (!missing.isEmpty()) {
                throw new NotFoundException(String.format("Clubs with ids = %s not found", missing));
            }
            return new HashSet<>(clubs);
        });
    }
}
