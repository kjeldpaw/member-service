package dk.wandywharang.repository;

import dk.wandywharang.entity.GraduationEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class GraduationRepository implements PanacheRepositoryBase<GraduationEntity, UUID> {
}
