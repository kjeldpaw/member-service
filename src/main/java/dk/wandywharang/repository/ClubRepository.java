package dk.wandywharang.repository;

import dk.wandywharang.entity.ClubEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClubRepository implements PanacheRepository<ClubEntity> {
}
