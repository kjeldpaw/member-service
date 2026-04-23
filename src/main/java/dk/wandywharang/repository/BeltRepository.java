package dk.wandywharang.repository;

import dk.wandywharang.entity.BeltEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BeltRepository implements PanacheRepositoryBase<BeltEntity, UUID> {
}
