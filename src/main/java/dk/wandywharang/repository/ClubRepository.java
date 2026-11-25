package dk.wandywharang.repository;

import dk.wandywharang.entity.ClubEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ClubRepository implements PanacheRepositoryBase<ClubEntity, UUID> {

    /**
     * Finds those of the given Clubs that exist.
     */
    public Uni<Set<ClubEntity>> findByIds(Set<UUID> ids) {
        return list("id in ?1", ids).map(HashSet::new);
    }
}
