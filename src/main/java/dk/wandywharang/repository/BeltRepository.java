package dk.wandywharang.repository;

import dk.wandywharang.entity.BeltEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BeltRepository implements PanacheRepository<BeltEntity> {
}
