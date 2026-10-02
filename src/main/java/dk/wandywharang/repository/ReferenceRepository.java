package dk.wandywharang.repository;

import dk.wandywharang.entity.ReferenceEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ReferenceRepository implements PanacheRepositoryBase<ReferenceEntity, UUID> {

    public Uni<ReferenceEntity> findByMember(UUID memberId, UUID referenceId) {
        return find("id = ?1 and member.id = ?2", referenceId, memberId).firstResult();
    }
}
