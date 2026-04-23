package dk.wandywharang.repository;

import dk.wandywharang.entity.ClubEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ClubRepository implements PanacheRepositoryBase<ClubEntity, UUID> {
}
