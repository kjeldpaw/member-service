package dk.wandywharang.repository;

import dk.wandywharang.entity.BeltEntity;
import dk.wandywharang.entity.MemberEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BeltRepository implements PanacheRepositoryBase<BeltEntity, UUID> {
}
